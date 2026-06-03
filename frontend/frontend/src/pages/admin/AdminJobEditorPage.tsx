import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Save, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import {
  adminCreateJob,
  adminGetJob,
  adminUpdateJob,
  adminUpdateStatus,
  type JobPayload,
} from '../../api/admin.api';
import { MarkdownToolbar } from '../../components/admin/MarkdownToolbar';
import { ChannelPanel } from '../../components/admin/ChannelPanel';

interface FormValues {
  title: string;
  description: string;
  responsibilities: string;
  requirements: string;
  benefits: string;
  seniority: string;
  employmentType: string;
  workMode: string;
  locationCity: string;
  locationState: string;
  locationCountry: string;
  department: string;
  jobCategory: string;
  salaryMin: string;
  salaryMax: string;
  currency: string;
  showSalary: boolean;
  applicationDeadline: string;
  metaTitle: string;
  metaDescription: string;
}

const today = new Date().toISOString().split('T')[0];

function buildPayload(v: FormValues): JobPayload {
  return {
    title: v.title,
    description: v.description || undefined,
    responsibilities: v.responsibilities || undefined,
    requirements: v.requirements || undefined,
    benefits: v.benefits || undefined,
    seniority: v.seniority,
    employmentType: v.employmentType || undefined,
    workMode: v.workMode || undefined,
    locationCity: v.locationCity || undefined,
    locationState: v.locationState || undefined,
    locationCountry: v.locationCountry || undefined,
    department: v.department || undefined,
    jobCategory: v.jobCategory || undefined,
    salaryMin: v.salaryMin ? Number(v.salaryMin) : undefined,
    salaryMax: v.salaryMax ? Number(v.salaryMax) : undefined,
    currency: v.currency || undefined,
    showSalary: v.showSalary,
    applicationDeadline: v.applicationDeadline,
    metaTitle: v.metaTitle || undefined,
    metaDescription: v.metaDescription || undefined,
  };
}

const SectionHeading = ({ children }: { children: React.ReactNode }) => (
  <h2 className="font-semibold text-slate-800 text-sm uppercase tracking-wide mb-3 mt-8 first:mt-0 pb-1 border-b border-slate-100">
    {children}
  </h2>
);

const FieldLabel = ({
  htmlFor,
  required,
  children,
}: {
  htmlFor: string;
  required?: boolean;
  children: React.ReactNode;
}) => (
  <label htmlFor={htmlFor} className="block text-sm font-medium text-slate-700 mb-1">
    {children}
    {required && <span className="text-red-500 ml-0.5">*</span>}
  </label>
);

const inputCls =
  'w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-400 focus:border-transparent placeholder-slate-300 transition';

const selectCls =
  'w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-400 focus:border-transparent transition';

export const AdminJobEditorPage = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const qc = useQueryClient();

  const {
    register,
    reset,
    setValue,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      seniority: 'MID',
      employmentType: 'FULL_TIME',
      workMode: 'ONSITE',
      currency: 'INR',
      showSalary: false,
      locationCountry: 'IN',
      description: '',
      responsibilities: '',
      requirements: '',
      benefits: '',
    },
  });

  // Refs for markdown toolbars — merged with react-hook-form refs
  const descRef = useRef<HTMLTextAreaElement>(null);
  const respRef = useRef<HTMLTextAreaElement>(null);
  const reqRef = useRef<HTMLTextAreaElement>(null);
  const benRef = useRef<HTMLTextAreaElement>(null);

  const mergeRef =
    (
      formRef: (el: HTMLTextAreaElement | null) => void,
      local: React.MutableRefObject<HTMLTextAreaElement | null>
    ) =>
    (el: HTMLTextAreaElement | null) => {
      formRef(el);
      local.current = el;
    };

  // Pre-fill form when editing
  const { data: existing, isLoading: loadingExisting } = useQuery({
    queryKey: ['admin-job', id],
    queryFn: () => adminGetJob(Number(id)),
    enabled: isEdit,
  });

  useEffect(() => {
    if (!existing) return;
    reset({
      title: existing.title ?? '',
      description: existing.description ?? '',
      responsibilities: existing.responsibilities ?? '',
      requirements: existing.requirements ?? '',
      benefits: existing.benefits ?? '',
      seniority: existing.seniority ?? 'MID',
      employmentType: existing.employmentType ?? 'FULL_TIME',
      workMode: existing.workMode ?? 'ONSITE',
      locationCity: existing.locationCity ?? '',
      locationState: existing.locationState ?? '',
      locationCountry: existing.locationCountry ?? 'IN',
      department: existing.department ?? '',
      jobCategory: existing.jobCategory ?? '',
      salaryMin: existing.salaryMin?.toString() ?? '',
      salaryMax: existing.salaryMax?.toString() ?? '',
      currency: existing.currency ?? 'INR',
      showSalary: existing.showSalary ?? false,
      applicationDeadline: existing.applicationDeadline ?? '',
      metaTitle: existing.metaTitle ?? '',
      metaDescription: existing.metaDescription ?? '',
    });
  }, [existing, reset]);

  const createMut = useMutation({
    mutationFn: adminCreateJob,
    onSuccess: (job) => {
      qc.invalidateQueries({ queryKey: ['admin-jobs'] });
      navigate(`/admin/jobs/${job.id}/edit`, { replace: true });
    },
  });

  const updateMut = useMutation({
    mutationFn: (payload: JobPayload) => adminUpdateJob(Number(id), payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-jobs'] });
      qc.invalidateQueries({ queryKey: ['admin-job', id] });
    },
  });

  const statusMut = useMutation({
    mutationFn: (status: string) => adminUpdateStatus(Number(id), status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-jobs'] });
      qc.invalidateQueries({ queryKey: ['admin-job', id] });
    },
  });

  const onSave = handleSubmit((values) => {
    const payload = buildPayload(values);
    if (isEdit) {
      updateMut.mutate(payload);
    } else {
      createMut.mutate(payload);
    }
  });

  const isBusy = createMut.isPending || updateMut.isPending || statusMut.isPending;
  const mutError = createMut.error || updateMut.error || statusMut.error;
  const currentStatus = existing?.status;

  // Determine which status actions are available
  const canPublish = isEdit && currentStatus === 'DRAFT';
  const canClose = isEdit && currentStatus === 'PUBLISHED';
  const saveLabel = updateMut.isSuccess ? 'Saved!' : isEdit ? 'Save Changes' : 'Save as Draft';

  const { ref: descFormRef, ...descReg } = register('description');
  const { ref: respFormRef, ...respReg } = register('responsibilities');
  const { ref: reqFormRef, ...reqReg } = register('requirements');
  const { ref: benFormRef, ...benReg } = register('benefits');

  if (isEdit && loadingExisting) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="space-y-3 animate-pulse">
          <div className="h-6 bg-slate-100 rounded w-1/3" />
          <div className="h-40 bg-slate-100 rounded-xl" />
          <div className="h-40 bg-slate-100 rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto pb-16">
      {/* Top bar */}
      <div className="flex items-center justify-between mb-6">
        <button
          onClick={() => navigate('/admin/jobs')}
          className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 transition-colors focus:outline-none focus:underline"
        >
          <ChevronLeft aria-hidden="true" className="w-4 h-4" />
          All Postings
        </button>

        {isEdit && currentStatus && (
          <span
            className={`text-xs font-semibold px-2.5 py-1 rounded-full ${
              currentStatus === 'DRAFT'
                ? 'bg-amber-100 text-amber-700'
                : currentStatus === 'PUBLISHED'
                ? 'bg-green-100 text-green-700'
                : 'bg-slate-100 text-slate-500'
            }`}
          >
            {currentStatus}
          </span>
        )}
      </div>

      <h1 className="font-display font-bold text-2xl text-slate-900 mb-6">
        {isEdit ? 'Edit Job Posting' : 'New Job Posting'}
      </h1>

      {mutError && (
        <div className="mb-4 flex items-start gap-2 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          <AlertCircle aria-hidden="true" className="w-4 h-4 mt-0.5 shrink-0" />
          <span>
            {(mutError as { response?: { data?: { message?: string } } })?.response?.data?.message ??
              'Something went wrong. Check backend is running on port 8086.'}
          </span>
        </div>
      )}

      <form onSubmit={onSave} noValidate>
        {/* ── Basic Info ─────────────────────────────────────────────────── */}
        <SectionHeading>Basic Info</SectionHeading>

        <div className="mb-4">
          <FieldLabel htmlFor="title" required>
            Job Title
          </FieldLabel>
          <input
            id="title"
            type="text"
            placeholder="e.g. Senior Java Developer"
            className={`${inputCls} ${errors.title ? 'border-red-400' : ''}`}
            {...register('title', { required: 'Title is required' })}
          />
          {errors.title && (
            <p className="text-xs text-red-500 mt-1">{errors.title.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
          <div>
            <FieldLabel htmlFor="seniority" required>
              Seniority
            </FieldLabel>
            <select id="seniority" className={selectCls} {...register('seniority', { required: true })}>
              {['JUNIOR', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE'].map((v) => (
                <option key={v} value={v}>{v}</option>
              ))}
            </select>
          </div>
          <div>
            <FieldLabel htmlFor="employmentType">Type</FieldLabel>
            <select id="employmentType" className={selectCls} {...register('employmentType')}>
              <option value="">Select…</option>
              {['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP'].map((v) => (
                <option key={v} value={v}>{v.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <FieldLabel htmlFor="workMode">Work Mode</FieldLabel>
            <select id="workMode" className={selectCls} {...register('workMode')}>
              <option value="">Select…</option>
              {['REMOTE', 'HYBRID', 'ONSITE'].map((v) => (
                <option key={v} value={v}>{v}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
          <div>
            <FieldLabel htmlFor="department">Department</FieldLabel>
            <input
              id="department"
              type="text"
              placeholder="e.g. Engineering"
              className={inputCls}
              {...register('department')}
            />
          </div>
          <div>
            <FieldLabel htmlFor="jobCategory">Job Category</FieldLabel>
            <input
              id="jobCategory"
              type="text"
              placeholder="e.g. Backend Development"
              className={inputCls}
              {...register('jobCategory')}
            />
          </div>
        </div>

        {/* ── Location ───────────────────────────────────────────────────── */}
        <SectionHeading>Location</SectionHeading>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
          <div>
            <FieldLabel htmlFor="locationCity">City</FieldLabel>
            <input
              id="locationCity"
              type="text"
              placeholder="e.g. Bangalore"
              className={inputCls}
              {...register('locationCity')}
            />
          </div>
          <div>
            <FieldLabel htmlFor="locationState">State / Region</FieldLabel>
            <input
              id="locationState"
              type="text"
              placeholder="e.g. KA"
              className={inputCls}
              {...register('locationState')}
            />
          </div>
          <div>
            <FieldLabel htmlFor="locationCountry">Country Code</FieldLabel>
            <input
              id="locationCountry"
              type="text"
              placeholder="e.g. IN"
              maxLength={3}
              className={inputCls}
              {...register('locationCountry')}
            />
          </div>
        </div>

        {/* ── Rich Content ───────────────────────────────────────────────── */}
        <SectionHeading>Content</SectionHeading>

        <div className="mb-4">
          <FieldLabel htmlFor="description">Role Summary</FieldLabel>
          <MarkdownToolbar
            textareaRef={descRef}
            onChange={(v) => setValue('description', v, { shouldDirty: true })}
          />
          <textarea
            id="description"
            rows={5}
            placeholder="Describe the role and its purpose…"
            className={inputCls}
            {...descReg}
            ref={mergeRef(descFormRef, descRef)}
          />
        </div>

        <div className="mb-4">
          <FieldLabel htmlFor="responsibilities">Responsibilities</FieldLabel>
          <MarkdownToolbar
            textareaRef={respRef}
            onChange={(v) => setValue('responsibilities', v, { shouldDirty: true })}
          />
          <textarea
            id="responsibilities"
            rows={5}
            placeholder="- Lead design of…&#10;- Collaborate with…"
            className={inputCls}
            {...respReg}
            ref={mergeRef(respFormRef, respRef)}
          />
          <p className="text-[11px] text-slate-400 mt-1">One item per line. Use `- ` for bullets.</p>
        </div>

        <div className="mb-4">
          <FieldLabel htmlFor="requirements">Requirements</FieldLabel>
          <MarkdownToolbar
            textareaRef={reqRef}
            onChange={(v) => setValue('requirements', v, { shouldDirty: true })}
          />
          <textarea
            id="requirements"
            rows={5}
            placeholder="- 5+ years Java&#10;- Experience with Spring Boot…"
            className={inputCls}
            {...reqReg}
            ref={mergeRef(reqFormRef, reqRef)}
          />
        </div>

        <div className="mb-4">
          <FieldLabel htmlFor="benefits">Benefits</FieldLabel>
          <MarkdownToolbar
            textareaRef={benRef}
            onChange={(v) => setValue('benefits', v, { shouldDirty: true })}
          />
          <textarea
            id="benefits"
            rows={4}
            placeholder="- Health insurance&#10;- Flexible hours…"
            className={inputCls}
            {...benReg}
            ref={mergeRef(benFormRef, benRef)}
          />
        </div>

        {/* ── Compensation ───────────────────────────────────────────────── */}
        <SectionHeading>Compensation</SectionHeading>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
          <div>
            <FieldLabel htmlFor="salaryMin">Min Salary</FieldLabel>
            <input
              id="salaryMin"
              type="number"
              min={0}
              placeholder="1800000"
              className={inputCls}
              {...register('salaryMin', { min: 0 })}
            />
          </div>
          <div>
            <FieldLabel htmlFor="salaryMax">Max Salary</FieldLabel>
            <input
              id="salaryMax"
              type="number"
              min={0}
              placeholder="2500000"
              className={inputCls}
              {...register('salaryMax', { min: 0 })}
            />
          </div>
          <div>
            <FieldLabel htmlFor="currency">Currency</FieldLabel>
            <select id="currency" className={selectCls} {...register('currency')}>
              {['INR', 'USD', 'EUR', 'GBP', 'AED'].map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <div className="flex flex-col justify-end pb-1">
            <label className="flex items-center gap-2 cursor-pointer text-sm text-slate-700">
              <input
                type="checkbox"
                className="w-4 h-4 rounded accent-primary-600"
                {...register('showSalary')}
              />
              Show salary
            </label>
          </div>
        </div>

        {/* ── Timing ─────────────────────────────────────────────────────── */}
        <SectionHeading>Timing</SectionHeading>

        <div className="mb-4 max-w-xs">
          <FieldLabel htmlFor="applicationDeadline" required>
            Application Deadline
          </FieldLabel>
          <input
            id="applicationDeadline"
            type="date"
            min={today}
            className={`${inputCls} ${errors.applicationDeadline ? 'border-red-400' : ''}`}
            {...register('applicationDeadline', {
              required: 'Deadline is required',
              validate: (v) => v > today || 'Deadline must be a future date',
            })}
          />
          {errors.applicationDeadline && (
            <p className="text-xs text-red-500 mt-1">{errors.applicationDeadline.message}</p>
          )}
        </div>

        {/* ── SEO (optional) ─────────────────────────────────────────────── */}
        <SectionHeading>SEO (optional)</SectionHeading>

        <div className="mb-4">
          <FieldLabel htmlFor="metaTitle">Meta Title</FieldLabel>
          <input
            id="metaTitle"
            type="text"
            placeholder="Senior Java Developer at Forge AI"
            maxLength={70}
            className={inputCls}
            {...register('metaTitle', { maxLength: 70 })}
          />
          <p className="text-[11px] text-slate-400 mt-1">
            {watch('metaTitle')?.length ?? 0} / 70
          </p>
        </div>

        <div className="mb-6">
          <FieldLabel htmlFor="metaDescription">Meta Description</FieldLabel>
          <textarea
            id="metaDescription"
            rows={2}
            placeholder="Brief description for search engines…"
            maxLength={160}
            className={inputCls}
            {...register('metaDescription', { maxLength: 160 })}
          />
          <p className="text-[11px] text-slate-400 mt-1">
            {watch('metaDescription')?.length ?? 0} / 160
          </p>
        </div>

        {/* ── Action bar ─────────────────────────────────────────────────── */}
        <div className="sticky bottom-0 -mx-6 lg:-mx-8 px-6 lg:px-8 py-4 bg-white border-t border-slate-200 flex flex-wrap items-center gap-3">
          <button
            type="submit"
            disabled={isBusy}
            className="btn-primary flex items-center gap-2 text-sm px-5 py-2.5 disabled:opacity-60"
          >
            <Save aria-hidden="true" className="w-4 h-4" />
            {saveLabel}
          </button>

          {canPublish && (
            <button
              type="button"
              disabled={isBusy}
              onClick={() => statusMut.mutate('PUBLISHED')}
              className="flex items-center gap-2 text-sm px-5 py-2.5 rounded-lg bg-green-600 text-white hover:bg-green-700 transition-colors disabled:opacity-60 font-medium"
            >
              <CheckCircle aria-hidden="true" className="w-4 h-4" />
              Publish
            </button>
          )}

          {canClose && (
            <button
              type="button"
              disabled={isBusy}
              onClick={() => statusMut.mutate('CLOSED')}
              className="flex items-center gap-2 text-sm px-5 py-2.5 rounded-lg bg-slate-200 text-slate-700 hover:bg-slate-300 transition-colors disabled:opacity-60 font-medium"
            >
              <XCircle aria-hidden="true" className="w-4 h-4" />
              Close Posting
            </button>
          )}

          {updateMut.isSuccess && (
            <span className="text-sm text-green-600 font-medium">Changes saved.</span>
          )}
          {statusMut.isSuccess && (
            <span className="text-sm text-green-600 font-medium">Status updated.</span>
          )}
        </div>
      </form>

      {/* REQ-JP-03 — channel management (only available once the posting exists) */}
      {isEdit && <ChannelPanel jobId={Number(id)} />}
    </div>
  );
};
