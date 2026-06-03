import { Info } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Props {
  jobTitle: string;
  onClose: () => void;
}

export const AlreadyAppliedModal = ({ jobTitle, onClose }: Props) => {
  const navigate = useNavigate();
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl p-8 max-w-sm w-full text-center shadow-xl">
        <div className="w-14 h-14 rounded-full bg-primary-100 flex items-center justify-center mx-auto mb-4">
          <Info className="w-7 h-7 text-primary-600" />
        </div>
        <h2 className="font-display font-bold text-xl text-slate-900 mb-2">Application Already Submitted</h2>
        <p className="text-slate-500 text-sm mb-6">
          It looks like you have already applied for the{' '}
          <span className="font-semibold text-slate-800">{jobTitle}</span> position. You can check the latest status of your application in your dashboard.
        </p>
        <button
          onClick={() => navigate('/applications')}
          className="w-full btn-primary py-2.5 mb-3"
        >
          Go to My Applications
        </button>
        <button onClick={onClose} className="text-sm text-slate-500 hover:text-slate-700 transition-colors">
          Close
        </button>
      </div>
    </div>
  );
};
