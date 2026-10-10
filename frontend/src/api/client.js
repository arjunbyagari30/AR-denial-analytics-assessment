const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

async function request(path, options) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);
  if (!response.ok) {
    let message = `Request failed with status ${response.status}.`;
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch {
      // Keep the status message when the server response isn't JSON.
    }
    throw new Error(message);
  }
  return response.json();
}

export function getPayerAnalytics(signal) {
  return request('/analytics/payers', { signal });
}

export function getReasonAnalytics(signal) {
  return request('/analytics/reasons', { signal });
}

export function getClaims(filters, signal) {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      query.set(key, String(value));
    }
  });
  return request(`/claims?${query.toString()}`, { signal });
}
