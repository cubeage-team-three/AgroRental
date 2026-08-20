import { Routes, Route, Navigate } from 'react-router-dom';

import MainLayout from '../layouts/MainLayout';
import DashboardLayout from '../layouts/DashboardLayout';
import AuthLayout from '../layouts/AuthLayout';
import PartnerLayout from '../layouts/PartnerLayout';

import Home from '../pages/Home';
import NotFound from '../pages/NotFound';

import Login from '../pages/auth/Login';
import Register from '../pages/auth/Register';
import RegisterFarmer from '../pages/auth/RegisterFarmer';
import RegisterPartner from '../pages/auth/RegisterPartner';
import RegisterOperator from '../pages/auth/RegisterOperator';
import VerifyOtp from '../pages/auth/VerifyOtp';

import FarmerDashboard from '../pages/farmer/FarmerDashboard';
import FarmerProfile from '../pages/farmer/FarmerProfile';
import MyFarms from '../pages/farmer/MyFarms';
import SearchEquipment from '../pages/farmer/SearchEquipment';
import EquipmentDetails from '../pages/farmer/EquipmentDetails';
import BookEquipment from '../pages/farmer/BookEquipment';
import MyBookings from '../pages/farmer/MyBookings';
import BookingDetails from '../pages/farmer/BookingDetails';
import CheckoutPayment from '../pages/farmer/CheckoutPayment';
import WriteReview from '../pages/farmer/WriteReview';
import FarmerPayments from '../pages/farmer/Payments';
import Invoice from '../pages/farmer/Invoice';
import LiveTracking from '../pages/farmer/LiveTracking';
import FarmerComplaints from '../pages/farmer/Complaints';
import FarmerNotifications from '../pages/farmer/Notifications';

import PartnerDashboard from '../pages/partner/PartnerDashboard';
import PartnerProfile from '../pages/partner/PartnerProfile';
import MyEquipment from '../pages/partner/MyEquipment';
import AddEquipment from '../pages/partner/AddEquipment';
import EquipmentAvailability from '../pages/partner/EquipmentAvailability';
import BookingRequests from '../pages/partner/BookingRequests';
import AssignOperator from '../pages/partner/AssignOperator';
import PartnerEarnings from '../pages/partner/Earnings';
import PartnerReviews from '../pages/partner/Reviews';
import PartnerNotifications from '../pages/partner/Notifications';

import OperatorDashboard from '../pages/operator/OperatorDashboard';
import OperatorProfile from '../pages/operator/OperatorProfile';
import AssignedJobs from '../pages/operator/AssignedJobs';
import JobDetails from '../pages/operator/JobDetails';
import OperatorEarnings from '../pages/operator/Earnings';
import JobHistory from '../pages/operator/JobHistory';
import OperatorRatings from '../pages/operator/Ratings';
import OperatorNotifications from '../pages/operator/Notifications';

import AdminDashboard from '../pages/admin/AdminDashboard';
import ManageFarmers from '../pages/admin/ManageFarmers';
import ManagePartners from '../pages/admin/ManagePartners';
import ManageOperators from '../pages/admin/ManageOperators';
import ManageEquipment from '../pages/admin/ManageEquipment';
import ManageBookings from '../pages/admin/ManageBookings';
import ManagePayments from '../pages/admin/ManagePayments';
import ManageComplaints from '../pages/admin/ManageComplaints';
import Reports from '../pages/admin/Reports';
import AdminNotifications from '../pages/admin/Notifications';
import SystemSettings from '../pages/admin/SystemSettings';
import AuditLogs from '../pages/admin/AuditLogs';


function AppRoutes() {
  return (
    <Routes>
      {/* Public / Auth routes */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="register/farmer" element={<RegisterFarmer />} />
        <Route path="register/partner" element={<RegisterPartner />} />
        <Route path="register/operator" element={<RegisterOperator />} />
        <Route path="verify-otp" element={<VerifyOtp />} />
      </Route>

      {/* Cinematic split-screen auth pages */}
      <Route element={<AuthLayout />}>
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
      </Route>

      {/* Farmer routes */}
      <Route path="/farmer" element={<DashboardLayout />}>
        <Route path="dashboard" element={<FarmerDashboard />} />
        <Route path="profile" element={<FarmerProfile />} />
        <Route path="farms" element={<MyFarms />} />
        <Route path="equipment" element={<SearchEquipment />} />
        <Route path="search-equipment" element={<SearchEquipment />} />
        <Route path="equipment/:id" element={<EquipmentDetails />} />
        <Route path="book/:equipmentId" element={<BookEquipment />} />
        <Route path="book-equipment" element={<BookEquipment />} />
        <Route path="bookings" element={<MyBookings />} />
        <Route path="my-bookings" element={<MyBookings />} />
        <Route path="bookings/:id" element={<BookingDetails />} />
        <Route path="bookings/:id/pay" element={<CheckoutPayment />} />
        <Route path="bookings/:id/invoice" element={<Invoice />} />
        <Route path="bookings/:id/review" element={<WriteReview />} />
        <Route path="bookings/:id/tracking" element={<LiveTracking />} />
        <Route path="live-tracking" element={<LiveTracking />} />
        <Route path="payments" element={<FarmerPayments />} />
        <Route path="complaints" element={<FarmerComplaints />} />
        <Route path="notifications" element={<FarmerNotifications />} />
      </Route>

      {/* Partner routes */}
      <Route path="/partner" element={<PartnerLayout />}>
        <Route path="dashboard" element={<PartnerDashboard />} />
        <Route path="profile" element={<PartnerProfile />} />
        <Route path="equipment" element={<MyEquipment />} />
        <Route path="my-equipment" element={<MyEquipment />} />
        <Route path="equipment/add" element={<AddEquipment />} />
        <Route path="add-equipment" element={<AddEquipment />} />
        <Route path="equipment/availability" element={<EquipmentAvailability />} />
        <Route path="bookings" element={<BookingRequests />} />
        <Route path="bookings/:id/assign-operator" element={<AssignOperator />} />
        <Route path="earnings" element={<PartnerEarnings />} />
        <Route path="reviews" element={<PartnerReviews />} />
        <Route path="notifications" element={<PartnerNotifications />} />
      </Route>

      {/* Operator routes */}
      <Route path="/operator" element={<DashboardLayout />}>
        <Route path="dashboard" element={<OperatorDashboard />} />
        <Route path="profile" element={<OperatorProfile />} />
        <Route path="jobs" element={<AssignedJobs />} />
        <Route path="jobs/:id" element={<JobDetails />} />
        <Route path="earnings" element={<OperatorEarnings />} />
        <Route path="history" element={<JobHistory />} />
        <Route path="ratings" element={<OperatorRatings />} />
        <Route path="notifications" element={<OperatorNotifications />} />
      </Route>

      {/* Admin routes */}
      <Route path="/admin" element={<DashboardLayout />}>
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="farmers" element={<ManageFarmers />} />
        <Route path="partners" element={<ManagePartners />} />
        <Route path="operators" element={<ManageOperators />} />
        <Route path="equipment" element={<ManageEquipment />} />
        <Route path="bookings" element={<ManageBookings />} />
        <Route path="payments" element={<ManagePayments />} />
        <Route path="complaints" element={<ManageComplaints />} />
        <Route path="reports" element={<Reports />} />
        <Route path="notifications" element={<AdminNotifications />} />
        <Route path="settings" element={<SystemSettings />} />
        <Route path="audit-logs" element={<AuditLogs />} />
      </Route>

      {/* Catch-all */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default AppRoutes;
