import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor attach token if present
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('tutorhub_access_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor handle common responses / errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 Unauthorized handling can be plugged in here
    return Promise.reject(error);
  }
);

export default apiClient;
