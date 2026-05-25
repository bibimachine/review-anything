import axios from 'axios';

const API_BASE = 'http://localhost:8000';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const configApi = {
  get: () => api.get('/api/config/'),
  save: (data) => api.post('/api/config/', data),
  delete: () => api.delete('/api/config/'),
  test: (data) => api.post('/api/config/test', data),
};

export const settingsApi = {
  get: () => api.get('/api/config/'),
  saveDailyCount: (count) => api.post('/api/config/', { daily_review_count: count }),
};

export const notesApi = {
  upload: (file, section = null, signal = null) => {
    const formData = new FormData();
    formData.append('file', file);
    const params = section ? { section } : {};
    return api.post('/api/notes/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params,
      signal,
    });
  },
  cancelUpload: () => api.post('/api/notes/upload-cancel'),
  getSections: () => api.get('/api/notes/sections'),
  getNotesBySection: (section) => api.get(`/api/notes/section/${encodeURIComponent(section)}`),
  deleteNote: (id) => api.delete(`/api/notes/${id}`),
  updateNote: (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.put(`/api/notes/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  regenerateQA: (section = null) => api.post('/api/notes/regenerate-qa', null, {
    params: section ? { section } : {},
  }),
};

export const sectionsApi = {
  list: () => api.get('/api/sections/'),
  create: (name) => api.post('/api/sections/', { name }),
  rename: (oldName, newName) => api.post('/api/sections/rename', { old_name: oldName, new_name: newName }),
  delete: (name) => api.delete(`/api/sections/${encodeURIComponent(name)}`),
};

export const reviewApi = {
  getNext: (section) => api.get('/api/review/next', { params: { section } }),
  getBatch: (section, count) => api.get('/api/review/batch', { params: { section, count } }),
  action: (id, action) => api.post(`/api/review/${id}/action`, { action }),
  getStats: () => api.get('/api/review/stats'),
};

export const checkinApi = {
  checkin: () => api.post('/api/checkin/'),
  getStatus: () => api.get('/api/checkin/'),
  getCalendar: (year, month) => api.get(`/api/checkin/calendar/${year}/${month}`),
};

export default api;
