import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProfile, updateProfile, uploadResume } from '../api/profile.api';
import { getProfile as getLocalProfile, saveProfile } from '../store/localStorage';
import { useAuthStore } from '../store/authStore';
import type { CandidateProfile } from '../features/jobs/types/job.types';
import { Edit2, Plus, X, FileText, Download, Eye } from 'lucide-react';
import { clsx } from 'clsx';

const Toggle = ({ checked, onChange }: { checked: boolean; onChange: (v: boolean) => void }) => (
  <button type="button" onClick={() => onChange(!checked)}
    className={clsx('relative w-10 h-6 rounded-full transition-colors duration-200', checked ? 'bg-primary-600' : 'bg-slate-200')}>
    <div className={clsx('absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform duration-200', checked ? 'translate-x-5' : 'translate-x-1')} />
  </button>
);

const FieldLabel = ({ label }: { label: string }) => (
  <label className="block text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</label>
);

export const ProfilePage = () => {
  const { isAuthenticated } = useAuthStore();
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [skillInput, setSkillInput] = useState('');
  const [locInput, setLocInput] = useState('');

  // Backend profile or localStorage fallback
  const { data: backendProfile } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: isAuthenticated,
  });

  const localProfile = getLocalProfile();
  const rawProfile = backendProfile ?? localProfile;

  // Normalize backend field names
  const profile: CandidateProfile = {
    name: (rawProfile as any).name ?? (rawProfile as any).fullName ?? '',
    email: (rawProfile as any).email ?? '',
    phone: (rawProfile as any).phone ?? '',
    bio: (rawProfile as any).bio ?? '',
    professionalTitle: (rawProfile as any).professionalTitle ?? (rawProfile as any).professional_title ?? '',
    resumeFileName: (rawProfile as any).resumeFileName ?? (rawProfile as any).resume_file_name ?? '',
    skills: (rawProfile as any).skills ?? [],
    preferredLocations: (rawProfile as any).preferredLocations ?? (rawProfile as any).preferred_locations ?? [],
    salaryExpectation: (rawProfile as any).salaryExpectation ?? (rawProfile as any).salary_expectation ?? '',
    workMode: (rawProfile as any).workMode ?? (rawProfile as any).work_mode ?? 'REMOTE',
    smartJobAlerts: (rawProfile as any).smartJobAlerts ?? (rawProfile as any).smart_job_alerts ?? true,
    applicationStatusUpdates: (rawProfile as any).applicationStatusUpdates ?? (rawProfile as any).app_status_updates ?? true,
    employerMessaging: (rawProfile as any).employerMessaging ?? (rawProfile as any).employer_messaging ?? false,
  };

  const [local, setLocal] = useState<CandidateProfile>(profile);
  const update = (key: keyof CandidateProfile, value: unknown) => setLocal((p) => ({ ...p, [key]: value }));

  const { mutate: saveChanges } = useMutation({
    mutationFn: async () => {
      saveProfile(local);
      if (isAuthenticated) await updateProfile(local);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profile'] });
      setEditing(false);
    },
  });

  const { mutate: handleResumeUpload } = useMutation({
    mutationFn: (file: File) => uploadResume(file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  });

  const addSkill = () => {
    if (skillInput.trim() && !local.skills.includes(skillInput.trim())) {
      update('skills', [...local.skills, skillInput.trim()]);
      setSkillInput('');
    }
  };
  const removeSkill = (s: string) => update('skills', local.skills.filter((x) => x !== s));
  const addLoc = () => {
    if (locInput.trim()) {
      update('preferredLocations', [...local.preferredLocations, locInput.trim()]);
      setLocInput('');
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-4">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Personal Info */}
        <div className="lg:col-span-2 card p-6">
          <div className="flex items-start justify-between mb-5">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <span className="w-7 h-7 bg-primary-100 text-primary-700 rounded-lg flex items-center justify-center text-sm">👤</span>
              Personal Information
            </h2>
            <button onClick={() => editing ? saveChanges() : setEditing(true)}
              className="flex items-center gap-1.5 text-xs text-primary-600 btn-secondary px-3 py-1.5">
              <Edit2 className="w-3.5 h-3.5" /> {editing ? 'Save Profile' : 'Edit Profile'}
            </button>
          </div>
          <div className="grid grid-cols-2 gap-4 mb-4">
            {[
              { key: 'name' as const, label: 'Full Name', placeholder: 'Your name' },
              { key: 'professionalTitle' as const, label: 'Professional Title', placeholder: 'e.g. Senior Engineer' },
              { key: 'email' as const, label: 'Email Address', placeholder: 'you@example.com' },
              { key: 'phone' as const, label: 'Phone Number', placeholder: '+1 (555) ...' },
            ].map(({ key, label, placeholder }) => (
              <div key={key}>
                <FieldLabel label={label} />
                {editing
                  ? <input value={local[key] as string ?? ''} onChange={(e) => update(key, e.target.value)} placeholder={placeholder}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300" />
                  : <p className="text-sm font-medium text-slate-900">{local[key] as string || '—'}</p>
                }
              </div>
            ))}
          </div>
          <FieldLabel label="Professional Bio" />
          {editing
            ? <textarea value={local.bio} onChange={(e) => update('bio', e.target.value)} rows={3}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300 resize-none" />
            : <p className="text-sm text-slate-600 leading-relaxed">{local.bio || '—'}</p>
          }
        </div>

        {/* Preferences */}
        <div className="card p-6">
          <h2 className="font-semibold text-slate-800 mb-4 flex items-center gap-2">
            <span className="w-7 h-7 bg-blue-100 text-blue-700 rounded-lg flex items-center justify-center text-sm">⚙</span>
            Preferences
          </h2>
          <div className="mb-4">
            <p className="text-xs text-slate-400 uppercase tracking-wide mb-2">Work Mode</p>
            <div className="flex gap-1.5 flex-wrap">
              {['REMOTE', 'HYBRID', 'ONSITE'].map((m) => (
                <button key={m} type="button" onClick={() => update('workMode', m)}
                  className={clsx('px-3 py-1 rounded-full text-xs font-medium transition-colors',
                    local.workMode === m ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200')}>
                  {m.charAt(0) + m.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>
          <div className="mb-4">
            <p className="text-xs text-slate-400 uppercase tracking-wide mb-2">Preferred Locations</p>
            {local.preferredLocations.map((loc) => (
              <div key={loc} className="flex items-center gap-1.5 text-xs text-slate-700 mb-1">
                <span>📍</span> {loc}
                {editing && <button onClick={() => update('preferredLocations', local.preferredLocations.filter((l) => l !== loc))}><X className="w-3 h-3 text-slate-400" /></button>}
              </div>
            ))}
            {editing && (
              <div className="flex gap-1.5 mt-2">
                <input value={locInput} onChange={(e) => setLocInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addLoc(); } }}
                  placeholder="Add location..." className="flex-1 text-xs border border-slate-200 rounded px-2 py-1 focus:outline-none focus:ring-1 focus:ring-primary-300" />
                <button type="button" onClick={addLoc} className="text-primary-600"><Plus className="w-4 h-4" /></button>
              </div>
            )}
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wide mb-2">Desired Salary Range</p>
            {editing
              ? <input value={local.salaryExpectation} onChange={(e) => update('salaryExpectation', e.target.value)} placeholder="e.g. $145k - $180k"
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300" />
              : <p className="text-sm font-semibold text-primary-700">{local.salaryExpectation || '—'}</p>
            }
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Resume */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800">📄 Resume</h2>
            <label className="text-xs text-primary-600 hover:text-primary-700 font-medium cursor-pointer">
              Replace
              <input type="file" accept=".pdf,.doc,.docx" className="hidden" onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) handleResumeUpload(f);
              }} />
            </label>
          </div>
          <div className="border-2 border-dashed border-slate-200 rounded-xl p-6 text-center">
            <FileText className="w-10 h-10 text-primary-400 mx-auto mb-2" />
            <p className="text-sm font-medium text-slate-700">{local.resumeFileName ?? 'No resume uploaded'}</p>
            {local.resumeFileName && <p className="text-xs text-slate-400 mt-1">Updated recently</p>}
          </div>
          {local.resumeFileName && (
            <div className="flex gap-2 mt-3">
              <button className="flex-1 btn-secondary text-xs py-2 flex items-center justify-center gap-1.5"><Eye className="w-3.5 h-3.5" /> Preview</button>
              <button className="flex-1 btn-secondary text-xs py-2 flex items-center justify-center gap-1.5"><Download className="w-3.5 h-3.5" /> Download</button>
            </div>
          )}
        </div>

        {/* Skills */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800">✦ Skills</h2>
            <p className="text-xs text-slate-400">Validated against workforce benchmarks</p>
          </div>
          <div className="flex flex-wrap gap-2 mb-4">
            {local.skills.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 bg-primary-100 text-primary-700 px-3 py-1.5 rounded-lg text-xs font-medium">
                {s}
                {editing && <button onClick={() => removeSkill(s)}><X className="w-3 h-3" /></button>}
              </span>
            ))}
          </div>
          {editing && (
            <div className="flex gap-2">
              <input value={skillInput} onChange={(e) => setSkillInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addSkill(); } }}
                placeholder="Add skill..." className="flex-1 text-xs border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-300" />
              <button type="button" onClick={addSkill} className="btn-primary px-3 py-2 text-xs">Add</button>
            </div>
          )}
        </div>
      </div>

      {/* Communication Prefs */}
      <div className="card p-6">
        <h2 className="font-semibold text-slate-800 mb-4">⚙️ Communication Preferences</h2>
        <div className="space-y-4">
          {[
            { key: 'smartJobAlerts' as const, label: 'Smart Job Alerts', desc: 'Receive AI-curated job recommendations via email weekly' },
            { key: 'applicationStatusUpdates' as const, label: 'Application Status Updates', desc: 'Real-time push notifications when your status changes' },
            { key: 'employerMessaging' as const, label: 'Employer Messaging', desc: 'Allow direct messaging from verified hiring managers' },
          ].map(({ key, label, desc }) => (
            <div key={key} className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-800">{label}</p>
                <p className="text-xs text-slate-500">{desc}</p>
              </div>
              <Toggle checked={!!local[key]} onChange={(v) => update(key, v)} />
            </div>
          ))}
        </div>
        {editing && (
          <button onClick={() => saveChanges()} className="btn-primary mt-4 px-5 py-2 text-sm">Save All Changes</button>
        )}
      </div>
    </div>
  );
};
