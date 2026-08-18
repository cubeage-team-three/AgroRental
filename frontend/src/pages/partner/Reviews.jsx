import { useState } from 'react';
import { Star, ThumbsUp, MessageSquare, Tractor, BadgeCheck } from 'lucide-react';

const MOCK_REVIEWS = [
  {
    id: 1,
    farmerName: 'Ramesh Yadav',
    rating: 5,
    date: '15 Aug 2026',
    equipment: 'Mahindra 575 DI Tractor',
    comment:
      'Excellent tractor condition! The machine was clean, fuel-efficient, and arrived on time with all attachments. Completed 5 acres of sugarcane ploughing effortlessly.',
    verified: true,
  },
  {
    id: 2,
    farmerName: 'Suresh Patil',
    rating: 5,
    date: '08 Aug 2026',
    equipment: 'John Deere Rotavator 6ft',
    comment:
      'Very cooperative partner. Machine was in brand new condition and worked smoothly throughout the day. Highly recommended for nearby farmers.',
    verified: true,
  },
  {
    id: 3,
    farmerName: 'Dnyaneshwar Shinde',
    rating: 4,
    date: '01 Aug 2026',
    equipment: 'Preet 987 Combined Harvester',
    comment:
      'Harvester performance was great. Good support by the operator. Small delay in morning arrival but work finished before schedule.',
    verified: true,
  },
];

function PartnerReviews() {
  return (
    <div className="max-w-6xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="border-b border-gray-200 pb-4">
        <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
          Machinery Reviews & Farmer Ratings
        </h1>
        <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
          Feedback and quality ratings from farmers who rented your machinery fleet.
        </p>
      </div>

      {/* Rating Overview Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 grid grid-cols-1 sm:grid-cols-3 gap-6 items-center">
        <div className="text-center sm:text-left space-y-1">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Fleet Rating</span>
          <div className="flex items-center justify-center sm:justify-start gap-2">
            <span className="text-4xl font-black text-[#142E1C]">4.9</span>
            <div className="text-amber-500 flex text-lg">★★★★★</div>
          </div>
          <p className="text-xs text-gray-500 font-medium">Based on 28 farmer reviews</p>
        </div>

        <div className="sm:col-span-2 space-y-2 text-xs">
          <div className="flex items-center gap-2">
            <span className="w-12 font-bold text-gray-600">5 Star</span>
            <div className="flex-1 bg-gray-100 h-2.5 rounded-full overflow-hidden">
              <div className="bg-[#3E7B27] h-full w-[90%]" />
            </div>
            <span className="w-8 font-bold text-gray-700 text-right">90%</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-12 font-bold text-gray-600">4 Star</span>
            <div className="flex-1 bg-gray-100 h-2.5 rounded-full overflow-hidden">
              <div className="bg-[#3E7B27] h-full w-[10%]" />
            </div>
            <span className="w-8 font-bold text-gray-700 text-right">10%</span>
          </div>
        </div>
      </div>

      {/* Reviews List */}
      <div className="space-y-4">
        {MOCK_REVIEWS.map((rev) => (
          <div
            key={rev.id}
            className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-3"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center gap-1.5">
                  <h4 className="text-sm font-extrabold text-gray-900">{rev.farmerName}</h4>
                  {rev.verified && <BadgeCheck className="w-4 h-4 text-emerald-600" />}
                </div>
                <span className="text-xs text-gray-500">{rev.equipment} • {rev.date}</span>
              </div>
              <div className="text-amber-500 font-bold text-xs">
                {'★'.repeat(rev.rating)}
              </div>
            </div>

            <p className="text-xs sm:text-sm text-gray-700 leading-relaxed font-medium">
              "{rev.comment}"
            </p>
          </div>
        ))}
      </div>

    </div>
  );
}

export default PartnerReviews;
