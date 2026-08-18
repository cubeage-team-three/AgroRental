import { request } from './apiClient';

/**
 * Service methods for Operator Profile Management (Module 5)
 */

/**
 * Fetch the profile of the currently authenticated operator.
 */
export async function getOperatorProfile() {
  return request('/operators/profile', {
    method: 'GET',
  });
}

/**
 * Update allowed profile fields (fullName, email, address, experience, skills, profilePhoto)
 * for the authenticated operator.
 */
export async function updateOperatorProfile(data) {
  return request('/operators/profile', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

/**
 * Upload a profile photo for the authenticated operator.
 */
export async function uploadOperatorProfilePhoto(file) {
  const formData = new FormData();
  formData.append('file', file);

  return request('/operators/profile/photo', {
    method: 'POST',
    body: formData,
  });
}
