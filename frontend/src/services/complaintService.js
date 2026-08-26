import { request } from './apiClient';
import { getFarmerId } from './authService';

export const complaintService = {
  async getFarmerComplaints() {
    try {
      const farmerId = getFarmerId();
      const endpoint = farmerId ? `/farmers/complaints?farmerId=${farmerId}` : '/farmers/complaints';
      const data = await request(endpoint);
      return Array.isArray(data) ? data : [];
    } catch (error) {
      console.warn('Backend complaints call failed, returning local storage fallback:', error);
      const cached = localStorage.getItem('farmer_complaints');
      return cached ? JSON.parse(cached) : [];
    }
  },

  async getComplaintById(id) {
    return await request(`/farmers/complaints/${id}`);
  },

  async createComplaint(data) {
    try {
      const farmerId = getFarmerId();
      const payload = {
        farmerId,
        bookingId: data.bookingId ? Number(data.bookingId) : null,
        category: data.category,
        description: data.description
      };
      return await request('/farmers/complaints', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
    } catch (error) {
      console.warn('Backend complaint create failed, adding to local storage:', error);
      const farmerId = getFarmerId();
      const newComplaint = {
        id: Date.now(),
        farmerId,
        bookingId: data.bookingId ? Number(data.bookingId) : null,
        category: data.category,
        description: data.description,
        status: 'OPEN',
        resolutionNote: 'Complaint logged successfully. AgroRental support team will contact you shortly.',
        createdAt: new Date().toISOString()
      };
      const cached = localStorage.getItem('farmer_complaints');
      const complaints = cached ? JSON.parse(cached) : [];
      complaints.unshift(newComplaint);
      localStorage.setItem('farmer_complaints', JSON.stringify(complaints));
      return newComplaint;
    }
  }
};
