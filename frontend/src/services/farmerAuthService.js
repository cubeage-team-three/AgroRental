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
  if (!farmerId) return null;
  return await apiRequest(`/farmers/profile/${farmerId}`, 'GET');
};

export const updateFarmerProfile = async (farmerId, profileData) => {
  if (!farmerId) throw new Error('Farmer ID required');
  return await apiRequest(`/farmers/profile/${farmerId}`, 'PUT', profileData);
};

export const changeFarmerPassword = async (farmerId, passwordData) => {
  if (!farmerId) throw new Error('Farmer ID required');
  return await apiRequest(`/farmers/change-password/${farmerId}`, 'PUT', passwordData);
};

export const getFarmerDashboard = async (farmerId) => {
  if (!farmerId) return null;
  return await apiRequest(`/farmers/dashboard?farmerId=${farmerId}`, 'GET');
};

