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
  const targetId = farmerId || 1;
  return await apiRequest(`/farmers/profile/${targetId}`, 'GET');
};

export const updateFarmerProfile = async (farmerId, profileData) => {
  const targetId = farmerId || 1;
  return await apiRequest(`/farmers/profile/${targetId}`, 'PUT', profileData);
};

export const changeFarmerPassword = async (farmerId, passwordData) => {
  const targetId = farmerId || 1;
  return await apiRequest(`/farmers/change-password/${targetId}`, 'PUT', passwordData);
};

export const getFarmerDashboard = async (farmerId) => {
  const targetId = farmerId || 1;
  return await apiRequest(`/farmers/dashboard?farmerId=${targetId}`, 'GET');
};

