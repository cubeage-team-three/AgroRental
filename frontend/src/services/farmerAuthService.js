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
  const query = farmerId ? `?farmerId=${farmerId}` : '';
  return await apiRequest(`/farmers/profile${query}`, 'GET');
};

export const updateFarmerProfile = async (farmerId, profileData) => {
  const query = farmerId ? `?farmerId=${farmerId}` : '';
  return await apiRequest(`/farmers/profile${query}`, 'PUT', profileData);
};

export const changeFarmerPassword = async (farmerId, passwordData) => {
  const query = farmerId ? `?farmerId=${farmerId}` : '';
  return await apiRequest(`/farmers/change-password${query}`, 'PUT', passwordData);
};

export const getFarmerDashboard = async (farmerId) => {
  const query = farmerId ? `?farmerId=${farmerId}` : '';
  return await apiRequest(`/farmers/dashboard${query}`, 'GET');
};

