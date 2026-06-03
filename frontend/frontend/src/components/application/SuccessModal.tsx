import { CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface Props {
  jobTitle: string;
}

export const SuccessPage = ({ jobTitle }: Props) => {
  const navigate = useNavigate();
  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-sm w-full overflow-hidden">
        <div className="h-1.5 bg-primary-600 w-full" />
        <div className="p-8 text-center">
          <div className="w-16 h-16 rounded-full border-2 border-primary-600 flex items-center justify-center mx-auto mb-5">
            <CheckCircle className="w-9 h-9 text-primary-600" />
          </div>
          <h2 className="font-display font-bold text-2xl text-slate-900 mb-2">Application Submitted Successfully!</h2>
          <p className="text-slate-500 text-sm mb-8">
            Your application for <span className="font-semibold text-slate-800">{jobTitle}</span> has been received. You can track your progress in the My Applications dashboard.
          </p>
          <button
            onClick={() => navigate('/applications')}
            className="w-full btn-primary py-3 mb-3"
          >
            View My Applications →
          </button>
          <button
            onClick={() => navigate('/')}
            className="text-sm text-slate-500 hover:text-slate-700 transition-colors"
          >
            Return to Careers Portal
          </button>
        </div>
      </div>
    </div>
  );
};
