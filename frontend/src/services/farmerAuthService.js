import { apiRequest } from './api';

export const registerFarmer = async (farmerData) => {
  return await apiRequest('/farmers/register', 'POST', farmerData);
};

export const sendOtp = async (mobileNumber) => {
  return await apiRequest('/farmers/send-otp', 'POST', { mobileNumber });
};

export const verifyOtp = async (mobileNumber, otp) => {
  return await apiRequest('/farmers/verify-otp', 'POST', { mobileNumber, otp });
};

export const resendOtp = async (mobileNumber) => {
  return await apiRequest('/farmers/resend-otp', 'POST', { mobileNumber });
};

export const getFarmerProfile = async (farmerId) => {
  if (farmerId) {
    return await apiRequest(`/farmers/profile/${farmerId}`, 'GET');
  }
  return await apiRequest('/farmers/profile', 'GET');
};

export const updateFarmerProfile = async (farmerId, profileData) => {
  if (farmerId) {
    return await apiRequest(`/farmers/profile/${farmerId}`, 'PUT', profileData);
  }
  return await apiRequest('/farmers/profile', 'PUT', profileData);
};

export const changeFarmerPassword = async (farmerId, passwordData) => {
  if (farmerId) {
    return await apiRequest(`/farmers/change-password/${farmerId}`, 'PUT', passwordData);
  }
  return await apiRequest('/farmers/change-password', 'PUT', passwordData);
};

export const uploadFarmerAvatar = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return await apiRequest('/farmers/profile/avatar', 'POST', formData);
};

export const getFarmerDashboard = async (farmerId) => {
  if (farmerId) {
    return await apiRequest(`/farmers/dashboard?farmerId=${farmerId}`, 'GET');
  }
  return await apiRequest('/farmers/dashboard', 'GET');
};

