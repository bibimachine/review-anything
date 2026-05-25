from datetime import datetime, timedelta
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from ..database import get_db
from ..models import CheckIn

router = APIRouter(prefix="/api/checkin", tags=["checkin"])


def _today_str() -> str:
    return datetime.utcnow().strftime("%Y-%m-%d")


def _calc_streak(db: Session) -> int:
    """计算连续打卡天数"""
    today = datetime.utcnow().date()
    streak = 0

    # 检查今天是否打卡，如果没打卡则从今天前一天开始算
    check_dates = set(
        r[0] for r in db.query(CheckIn.checkin_date).all()
    )

    # 从今天开始往前数
    for i in range(365 * 10):  # 最多查10年
        d = today - timedelta(days=i)
        d_str = d.strftime("%Y-%m-%d")
        if d_str in check_dates:
            streak += 1
        else:
            # 如果是今天还没打卡，继续往前看
            if i == 0:
                continue
            break

    return streak


@router.post("/")
def checkin(db: Session = Depends(get_db)):
    """今日打卡"""
    today = _today_str()
    existing = db.query(CheckIn).filter(CheckIn.checkin_date == today).first()
    if existing:
        return {"message": "今日已打卡", "checkin_date": today, "streak": _calc_streak(db)}

    record = CheckIn(checkin_date=today)
    db.add(record)
    db.commit()
    return {"message": "打卡成功", "checkin_date": today, "streak": _calc_streak(db)}


@router.get("/")
def get_checkin_status(db: Session = Depends(get_db)):
    """获取打卡状态：今日是否打卡 + 连续天数 + 所有打卡日期"""
    today = _today_str()
    checked_today = db.query(CheckIn).filter(CheckIn.checkin_date == today).first() is not None

    records = db.query(CheckIn.checkin_date).order_by(CheckIn.checkin_date.desc()).all()
    dates = [r[0] for r in records]

    return {
        "checked_today": checked_today,
        "streak": _calc_streak(db),
        "dates": dates,
    }


@router.get("/calendar/{year}/{month}")
def get_month_calendar(year: int, month: int, db: Session = Depends(get_db)):
    """获取某月的打卡情况"""
    prefix = f"{year:04d}-{month:02d}"
    records = db.query(CheckIn.checkin_date).filter(
        CheckIn.checkin_date.like(f"{prefix}%")
    ).all()
    dates = [r[0] for r in records]
    return {"year": year, "month": month, "checked_dates": dates}
