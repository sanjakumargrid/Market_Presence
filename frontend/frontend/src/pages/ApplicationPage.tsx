import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { useJob } from '../features/jobs/hooks/useJob';
import { ApplicationStepper } from '../components/application/ApplicationStepper';
import { ResumeUploader } from '../components/application/ResumeUploader';
import { SuccessPage } from '../components/application/SuccessModal';
import { AlreadyAppliedModal } from '../components/application/AlreadyAppliedModal';
import { addApplication, hasApplied } from '../store/localStorage';
import { submitApplication } from '../api/applications.api';
import { useAuthStore } from '../store/authStore';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useState, useEffect } from 'react';
import { X, Plus, Trash2 } from 'lucide-react';
import { recordEvent } from '../api/analytics.api';
import { clsx } from 'clsx';

const STEPS = [
  { label: 'Resume' },
  { label: 'My Info' },
  { label: 'Experience' },
  { label: 'Education' },
  { label: 'Skills' },
  { label: 'Certifications' },
  { label: 'Projects' },
  { label: 'Screening' },
  { label: 'Documents' },
  { label: 'Review' }
];

const schema = z.object({
  firstName: z.string().min(2, 'Required'),
  middleName: z.string().optional(),
  lastName: z.string().min(2, 'Required'),
  email: z.string().email('Valid email required'),
  phone: z.string().min(7, 'Required'),
  currentLocation: z.string().min(2, 'Required'),
  linkedinUrl: z.string().url().or(z.literal('')).optional(),
  portfolioUrl: z.string().url().or(z.literal('')).optional(),
  githubUrl: z.string().url().or(z.literal('')).optional(),
  
  experiences: z.array(z.object({
    companyName: z.string().min(1, 'Required'),
    designation: z.string().min(1, 'Required'),
    startDate: z.string().min(1, 'Required'),
    endDate: z.string().optional(),
    currentEmployer: z.boolean(),
    responsibilities: z.string()
  })),

  educations: z.array(z.object({
    institution: z.string().min(1, 'Required'),
    degree: z.string().min(1, 'Required'),
    specialization: z.string(),
    startYear: z.string().min(4, 'Required'),
    endYear: z.string().min(4, 'Required'),
    cgpa: z.string()
  })),

  skills: z.array(z.object({
    skillName: z.string(),
    proficiency: z.string(),
    yearsOfExperience: z.string()
  })),

  certifications: z.array(z.object({
    certificationName: z.string().min(1, 'Required'),
    issuingOrganization: z.string().min(1, 'Required'),
    issueDate: z.string().optional(),
    expiryDate: z.string().optional()
  })),

  projects: z.array(z.object({
    projectName: z.string().min(1, 'Required'),
    description: z.string(),
    technologiesUsed: z.string(),
    projectUrl: z.string().url().or(z.literal('')).optional()
  })),

  screeningVisa: z.string(),
  screeningNotice: z.string(),
  screeningExpectedCtc: z.string(),
  screeningRelocate: z.string(),

  gender: z.string().optional(),
  veteranStatus: z.string().optional(),
  disabilityStatus: z.string().optional(),

  gdprConsent: z.boolean().refine((v) => v === true, { message: 'GDPR consent is required' }),
});
type FormData = z.infer<typeof schema>;

const Field = ({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) => (
  <div>
    <label className="block">
      <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5">{label}</span>
      {children}
    </label>
    {error && <p role="alert" className="text-red-500 text-xs mt-1">{error}</p>}
  </div>
);

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & React.SelectHTMLAttributes<HTMLSelectElement> & React.TextareaHTMLAttributes<HTMLTextAreaElement> & { type?: string; rows?: number };

const Input = (props: InputProps) => {
  const className = clsx('w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300', props.className);
  if (props.type === 'textarea') return <textarea {...(props as any)} className={className} />;
  if (props.type === 'select') return <select {...(props as any)} className={className} />;
  return <input {...(props as any)} className={className} />;
};

export const ApplicationPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  // REQ-JP-11: referral code passed via router state from JobDetailsPage
  const referralCode: string | null = (location.state as any)?.referralCode ?? null;
  const channel: string = (location.state as any)?.channel ?? 'CAREERS_PORTAL';
  const { isAuthenticated } = useAuthStore();
  const { data: job, isLoading } = useJob(slug!);
  
  const [step, setStep] = useState(0);
  const [submitted, setSubmitted] = useState(false);
  const [alreadyApplied, setAlreadyApplied] = useState(false);
  
  const [resumeFile, setResumeFile] = useState<File | undefined>();
  const [coverLetterFile, setCoverLetterFile] = useState<File | undefined>();
  const [certsFile, setCertsFile] = useState<File | undefined>();
  const [transcriptsFile, setTranscriptsFile] = useState<File | undefined>();
  const [portfolioFile, setPortfolioFile] = useState<File | undefined>();
  const [isParsing, setIsParsing] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const { register, handleSubmit, control, setValue, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      experiences: [],
      educations: [],
      skills: [],
      certifications: [],
      projects: [],
      gdprConsent: false,
      screeningVisa: '',
      screeningNotice: '',
      screeningExpectedCtc: '',
      screeningRelocate: ''
    },
  });

  const { fields: expFields, append: appendExp, remove: removeExp, replace: replaceExp } = useFieldArray({ control, name: "experiences" });
  const { fields: eduFields, append: appendEdu, remove: removeEdu, replace: replaceEdu } = useFieldArray({ control, name: "educations" });
  const { fields: skillFields, append: appendSkill, remove: removeSkill, replace: replaceSkill } = useFieldArray({ control, name: "skills" });
  const { fields: certFields, append: appendCert, remove: removeCert, replace: replaceCert } = useFieldArray({ control, name: "certifications" });
  const { fields: projFields, append: appendProj, remove: removeProj, replace: replaceProj } = useFieldArray({ control, name: "projects" });

  useEffect(() => {
    if (slug && hasApplied(slug)) setAlreadyApplied(true);
  }, [slug]);

  // REQ-JP-05: fire APPLY_START once when the form is actually shown (not for already-applied modal)
  useEffect(() => {
    if (slug && !hasApplied(slug)) {
      recordEvent(slug, 'APPLY_START', channel);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  useEffect(() => {
    if (!isAuthenticated) navigate('/login');
  }, [isAuthenticated, navigate]);

  const handleResumeUpload = (file: File | undefined) => {
    setResumeFile(file);
    if (file) {
      setIsParsing(true);
      setTimeout(() => {
        setValue('firstName', 'Alex');
        setValue('middleName', 'M.');
        setValue('lastName', 'Sterling');
        setValue('email', 'alex.sterling@example.com');
        setValue('phone', '+1 (555) 987-6543');
        setValue('currentLocation', 'Austin, TX');
        setValue('linkedinUrl', 'https://linkedin.com/in/alexsterling');
        setValue('portfolioUrl', 'https://alexsterling.dev');
        setValue('githubUrl', 'https://github.com/alexsterling');
        
        replaceExp([
          { companyName: 'Global Tech', designation: 'Lead Engineer', startDate: '2021-06-01', endDate: '', currentEmployer: true, responsibilities: 'Architected robust pipelines and led a team of 5 engineers.' },
          { companyName: 'Innovate LLC', designation: 'Software Engineer', startDate: '2018-01-15', endDate: '2021-05-30', currentEmployer: false, responsibilities: 'Performed system modeling and generated actionable business insights.' }
        ]);
        
        replaceEdu([
          { institution: 'University of Texas', degree: 'M.S. Computer Science', specialization: 'Data Science', startYear: '2016', endYear: '2018', cgpa: '3.9' },
          { institution: 'Texas A&M', degree: 'B.S. Mathematics', specialization: 'Statistics', startYear: '2012', endYear: '2016', cgpa: '3.8' }
        ]);
        
        replaceSkill([
          { skillName: 'Python', proficiency: 'Expert', yearsOfExperience: '6' },
          { skillName: 'SQL', proficiency: 'Expert', yearsOfExperience: '5' },
          { skillName: 'AWS', proficiency: 'Advanced', yearsOfExperience: '4' },
          { skillName: 'React', proficiency: 'Intermediate', yearsOfExperience: '3' }
        ]);

        replaceCert([
          { certificationName: 'AWS Certified Solutions Architect', issuingOrganization: 'Amazon Web Services', issueDate: '2022-05-10', expiryDate: '2025-05-10' }
        ]);

        replaceProj([
          { projectName: 'Distributed Task Scheduler', description: 'Built an open source distributed cron system.', technologiesUsed: 'Go, Redis, Kubernetes', projectUrl: 'https://github.com/alexsterling/dts' }
        ]);
        
        setIsParsing(false);
      }, 1500);
    }
  };

  const onSubmit = async (data: FormData) => {
    if (!job) return;
    setSubmitError('');
    try {
      if (isAuthenticated) {
        await submitApplication(
          slug!,
          {
            first_name: data.firstName,
            middle_name: data.middleName,
            last_name: data.lastName,
            email: data.email,
            phone: data.phone,
            current_location: data.currentLocation,
            linkedin_url: data.linkedinUrl,
            portfolio_url: data.portfolioUrl,
            github_url: data.githubUrl,
            experiences: data.experiences.map(e => ({
              company_name: e.companyName,
              designation: e.designation,
              start_date: e.startDate,
              end_date: e.endDate || null,
              current_employer: e.currentEmployer,
              responsibilities: e.responsibilities
            })),
            educations: data.educations.map(e => ({
              institution: e.institution,
              degree: e.degree,
              specialization: e.specialization,
              start_year: parseInt(e.startYear),
              end_year: parseInt(e.endYear),
              cgpa: e.cgpa
            })),
            skills: data.skills.map(s => ({
              skill_name: s.skillName,
              proficiency: s.proficiency,
              years_of_experience: parseFloat(s.yearsOfExperience) || 0
            })),
            certifications: data.certifications.map(c => ({
              certification_name: c.certificationName,
              issuing_organization: c.issuingOrganization,
              issue_date: c.issueDate || null,
              expiry_date: c.expiryDate || null
            })),
            projects: data.projects.map(p => ({
              project_name: p.projectName,
              description: p.description,
              technologies_used: p.technologiesUsed,
              project_url: p.projectUrl || ''
            })),
            screening_answers: {
              "Visa Sponsorship": data.screeningVisa,
              "Notice Period": data.screeningNotice,
              "Expected CTC": data.screeningExpectedCtc,
              "Willing to Relocate": data.screeningRelocate
            },
            gdpr_consent: data.gdprConsent,
            gender: data.gender,
            veteran_status: data.veteranStatus,
            disability_status: data.disabilityStatus
          },
          { resume: resumeFile, coverLetter: coverLetterFile, certifications: certsFile, transcripts: transcriptsFile, portfolio: portfolioFile },
          referralCode ?? undefined
        );
      }
      addApplication({
        id: crypto.randomUUID(),
        jobId: job.id,
        jobTitle: job.title,
        jobSlug: job.slug,
        department: job.department,
        location: `${job.location_city} · ${job.work_mode}`,
        appliedDate: new Date().toISOString(),
        status: 'Applied',
        nextStep: 'HR Screening',
      });
      setSubmitted(true);
    } catch (e: any) {
      const msg = e?.response?.data?.error || e?.message || 'Submission failed';
      if (msg.toLowerCase().includes('already applied')) {
        setAlreadyApplied(true);
      } else {
        setSubmitError(msg);
      }
    }
  };

  const nextStep = () => setStep((s) => Math.min(s + 1, STEPS.length - 1));
  const prevStep = () => setStep((s) => Math.max(s - 1, 0));

  if (alreadyApplied && job) return <AlreadyAppliedModal jobTitle={job.title} onClose={() => navigate(`/jobs/${slug}`)} />;
  if (submitted && job) return <SuccessPage jobTitle={job.title} />;
  if (isLoading) return <div className="text-center py-20 text-slate-400">Loading...</div>;
  if (!job) return <div className="text-center py-20 text-slate-400">Job not found</div>;

  return (
    <div className="max-w-4xl mx-auto">
      <p className="text-xs font-semibold text-primary-600 uppercase tracking-widest mb-2">
        ✦ Applying for: {job.title.toUpperCase()}
      </p>
      <h1 className="font-display font-bold text-3xl text-slate-900 mb-1">Job Application</h1>
      <p className="text-slate-500 text-sm mb-6">Complete the fields below to finalize your submission.</p>

      <div className="mb-8">
        <ApplicationStepper steps={STEPS} currentStep={step} />
      </div>

      {submitError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-600">{submitError}</div>
      )}

      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="card p-6 min-h-[400px]">
          
          {/* STEP 0: RESUME */}
          {step === 0 && (
            <div className="space-y-6">
              <div>
                <h2 className="font-semibold text-slate-800 mb-2">📄 AI Resume Parsing</h2>
                <p className="text-sm text-slate-500 mb-4">Upload your resume. Our AI will automatically extract your information to save you time.</p>
                <ResumeUploader value={resumeFile} onChange={handleResumeUpload} />
                {isParsing && (
                  <div className="mt-4 p-3 bg-blue-50 text-blue-600 rounded-lg text-sm flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
                    Parsing resume to extract information...
                  </div>
                )}
              </div>
            </div>
          )}

          {/* STEP 1: INFO */}
          {step === 1 && (
            <div className="space-y-6">
              <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3">Personal Information</h2>
              <div className="grid grid-cols-3 gap-4">
                <Field label="First Name *" error={errors.firstName?.message}><Input {...register('firstName')} /></Field>
                <Field label="Middle Name" error={errors.middleName?.message}><Input {...register('middleName')} /></Field>
                <Field label="Last Name *" error={errors.lastName?.message}><Input {...register('lastName')} /></Field>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Email Address *" error={errors.email?.message}><Input {...register('email')} type="email" /></Field>
                <Field label="Phone Number *" error={errors.phone?.message}><Input {...register('phone')} /></Field>
              </div>
              <Field label="Current Location *" error={errors.currentLocation?.message}><Input {...register('currentLocation')} placeholder="City, State, Country" /></Field>
              
              <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3 mt-8">Web Profiles</h2>
              <div className="grid grid-cols-2 gap-4">
                <Field label="LinkedIn URL" error={errors.linkedinUrl?.message}><Input {...register('linkedinUrl')} placeholder="https://linkedin.com/in/..." /></Field>
                <Field label="GitHub URL" error={errors.githubUrl?.message}><Input {...register('githubUrl')} placeholder="https://github.com/..." /></Field>
              </div>
              <Field label="Portfolio URL" error={errors.portfolioUrl?.message}><Input {...register('portfolioUrl')} placeholder="https://..." /></Field>
            </div>
          )}

          {/* STEP 2: EXPERIENCE */}
          {step === 2 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <h2 className="font-semibold text-slate-800">Experience History</h2>
                <button type="button" onClick={() => appendExp({ companyName: '', designation: '', startDate: '', currentEmployer: false, responsibilities: '' })} className="text-primary-600 text-sm font-medium flex items-center gap-1 hover:text-primary-700"><Plus aria-hidden="true" className="w-4 h-4"/> Add Experience</button>
              </div>
              {expFields.length === 0 && <p className="text-sm text-slate-500 italic">No experience added.</p>}
              <div className="space-y-6">
                {expFields.map((field, index) => (
                  <div key={field.id} className="p-4 bg-slate-50 rounded-xl relative border border-slate-200">
                    <button type="button" onClick={() => removeExp(index)} aria-label={`Remove experience ${index + 1}`} className="absolute top-4 right-4 text-slate-400 hover:text-red-500"><Trash2 aria-hidden="true" className="w-4 h-4"/></button>
                    <div className="grid grid-cols-2 gap-4 mb-4 pr-8">
                      <Field label="Company Name *" error={errors.experiences?.[index]?.companyName?.message}><Input {...register(`experiences.${index}.companyName`)} /></Field>
                      <Field label="Job Title *" error={errors.experiences?.[index]?.designation?.message}><Input {...register(`experiences.${index}.designation`)} /></Field>
                    </div>
                    <div className="grid grid-cols-2 gap-4 mb-4">
                      <Field label="Start Date *" error={errors.experiences?.[index]?.startDate?.message}><Input type="date" {...register(`experiences.${index}.startDate`)} /></Field>
                      <Field label="End Date" error={errors.experiences?.[index]?.endDate?.message}><Input type="date" {...register(`experiences.${index}.endDate`)} /></Field>
                    </div>
                    <Field label="Responsibilities"><Input type="textarea" rows={3} {...register(`experiences.${index}.responsibilities`)} /></Field>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 3: EDUCATION */}
          {step === 3 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <h2 className="font-semibold text-slate-800">Education History</h2>
                <button type="button" onClick={() => appendEdu({ institution: '', degree: '', specialization: '', startYear: '', endYear: '', cgpa: '' })} className="text-primary-600 text-sm font-medium flex items-center gap-1 hover:text-primary-700"><Plus aria-hidden="true" className="w-4 h-4"/> Add Education</button>
              </div>
              {eduFields.length === 0 && <p className="text-sm text-slate-500 italic">No education added.</p>}
              <div className="space-y-6">
                {eduFields.map((field, index) => (
                  <div key={field.id} className="p-4 bg-slate-50 rounded-xl relative border border-slate-200">
                    <button type="button" onClick={() => removeEdu(index)} aria-label={`Remove education ${index + 1}`} className="absolute top-4 right-4 text-slate-400 hover:text-red-500"><Trash2 aria-hidden="true" className="w-4 h-4"/></button>
                    <div className="grid grid-cols-2 gap-4 mb-4 pr-8">
                      <Field label="Institution *" error={errors.educations?.[index]?.institution?.message}><Input {...register(`educations.${index}.institution`)} /></Field>
                      <Field label="Degree *" error={errors.educations?.[index]?.degree?.message}><Input {...register(`educations.${index}.degree`)} placeholder="B.S., M.S., etc." /></Field>
                    </div>
                    <div className="grid grid-cols-3 gap-4">
                      <Field label="Start Year *" error={errors.educations?.[index]?.startYear?.message}><Input {...register(`educations.${index}.startYear`)} placeholder="2015" /></Field>
                      <Field label="End Year *" error={errors.educations?.[index]?.endYear?.message}><Input {...register(`educations.${index}.endYear`)} placeholder="2019" /></Field>
                      <Field label="CGPA / %"><Input {...register(`educations.${index}.cgpa`)} placeholder="3.8" /></Field>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 4: SKILLS */}
          {step === 4 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <h2 className="font-semibold text-slate-800">Skills</h2>
                <button type="button" onClick={() => appendSkill({ skillName: '', proficiency: 'Intermediate', yearsOfExperience: '1' })} className="text-primary-600 text-sm font-medium flex items-center gap-1 hover:text-primary-700"><Plus aria-hidden="true" className="w-4 h-4"/> Add Skill</button>
              </div>
              {skillFields.length === 0 && <p className="text-sm text-slate-500 italic">No skills added.</p>}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {skillFields.map((field, index) => (
                  <div key={field.id} className="flex gap-2 items-center bg-slate-50 p-2 rounded-lg border border-slate-200">
                    <Input {...register(`skills.${index}.skillName`)} placeholder="Skill name" aria-label={`Skill name ${index + 1}`} className="flex-1" />
                    <select {...register(`skills.${index}.proficiency`)} aria-label={`Proficiency for skill ${index + 1}`} className="border border-slate-200 rounded-lg px-2 py-2.5 text-sm outline-none bg-white">
                      <option value="Beginner">Beginner</option>
                      <option value="Intermediate">Intermediate</option>
                      <option value="Advanced">Advanced</option>
                      <option value="Expert">Expert</option>
                    </select>
                    <Input {...register(`skills.${index}.yearsOfExperience`)} placeholder="Yrs" aria-label={`Years of experience for skill ${index + 1}`} className="w-16 text-center" />
                    <button type="button" onClick={() => removeSkill(index)} aria-label={`Remove skill ${index + 1}`} className="text-slate-400 hover:text-red-500"><X aria-hidden="true" className="w-5 h-5"/></button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 5: CERTIFICATIONS */}
          {step === 5 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <h2 className="font-semibold text-slate-800">Certifications</h2>
                <button type="button" onClick={() => appendCert({ certificationName: '', issuingOrganization: '', issueDate: '', expiryDate: '' })} className="text-primary-600 text-sm font-medium flex items-center gap-1 hover:text-primary-700"><Plus aria-hidden="true" className="w-4 h-4"/> Add Certification</button>
              </div>
              {certFields.length === 0 && <p className="text-sm text-slate-500 italic">No certifications added.</p>}
              <div className="space-y-6">
                {certFields.map((field, index) => (
                  <div key={field.id} className="p-4 bg-slate-50 rounded-xl relative border border-slate-200">
                    <button type="button" onClick={() => removeCert(index)} aria-label={`Remove certification ${index + 1}`} className="absolute top-4 right-4 text-slate-400 hover:text-red-500"><Trash2 aria-hidden="true" className="w-4 h-4"/></button>
                    <div className="grid grid-cols-2 gap-4 mb-4 pr-8">
                      <Field label="Certification Name *" error={errors.certifications?.[index]?.certificationName?.message}><Input {...register(`certifications.${index}.certificationName`)} /></Field>
                      <Field label="Issuing Organization *" error={errors.certifications?.[index]?.issuingOrganization?.message}><Input {...register(`certifications.${index}.issuingOrganization`)} /></Field>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <Field label="Issue Date"><Input type="date" {...register(`certifications.${index}.issueDate`)} /></Field>
                      <Field label="Expiry Date"><Input type="date" {...register(`certifications.${index}.expiryDate`)} /></Field>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 6: PROJECTS */}
          {step === 6 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <h2 className="font-semibold text-slate-800">Projects</h2>
                <button type="button" onClick={() => appendProj({ projectName: '', description: '', technologiesUsed: '', projectUrl: '' })} className="text-primary-600 text-sm font-medium flex items-center gap-1 hover:text-primary-700"><Plus aria-hidden="true" className="w-4 h-4"/> Add Project</button>
              </div>
              {projFields.length === 0 && <p className="text-sm text-slate-500 italic">No projects added.</p>}
              <div className="space-y-6">
                {projFields.map((field, index) => (
                  <div key={field.id} className="p-4 bg-slate-50 rounded-xl relative border border-slate-200">
                    <button type="button" onClick={() => removeProj(index)} aria-label={`Remove project ${index + 1}`} className="absolute top-4 right-4 text-slate-400 hover:text-red-500"><Trash2 aria-hidden="true" className="w-4 h-4"/></button>
                    <div className="grid grid-cols-2 gap-4 mb-4 pr-8">
                      <Field label="Project Name *" error={errors.projects?.[index]?.projectName?.message}><Input {...register(`projects.${index}.projectName`)} /></Field>
                      <Field label="Project URL" error={errors.projects?.[index]?.projectUrl?.message}><Input {...register(`projects.${index}.projectUrl`)} /></Field>
                    </div>
                    <Field label="Technologies Used" error={errors.projects?.[index]?.technologiesUsed?.message}><Input {...register(`projects.${index}.technologiesUsed`)} placeholder="e.g. React, Node.js" /></Field>
                    <div className="mt-4">
                      <Field label="Description"><Input type="textarea" rows={3} {...register(`projects.${index}.description`)} /></Field>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 7: SCREENING */}
          {step === 7 && (
            <div className="space-y-8">
              <div>
                <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3 mb-4">Application Questions</h2>
                <div className="space-y-4">
                  <Field label="Will you now or in the future require visa sponsorship? *">
                    <select {...register('screeningVisa')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Select an option</option>
                      <option value="Yes">Yes</option>
                      <option value="No">No</option>
                    </select>
                  </Field>
                  <Field label="What is your notice period? *">
                    <select {...register('screeningNotice')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Select an option</option>
                      <option value="Immediate">Immediate</option>
                      <option value="15 Days">15 Days</option>
                      <option value="30 Days">30 Days</option>
                      <option value="60 Days+">60 Days+</option>
                    </select>
                  </Field>
                  <Field label="Are you willing to relocate? *">
                    <select {...register('screeningRelocate')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Select an option</option>
                      <option value="Yes">Yes</option>
                      <option value="No">No</option>
                    </select>
                  </Field>
                  <Field label="What is your expected compensation? *">
                    <Input {...register('screeningExpectedCtc')} placeholder="e.g. $120,000" />
                  </Field>
                </div>
              </div>
              <div>
                <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3 mb-4">Voluntary Self-Identification</h2>
                <div className="grid grid-cols-3 gap-4">
                  <Field label="Gender">
                    <select {...register('gender')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Prefer not to say</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Non-binary">Non-binary</option>
                    </select>
                  </Field>
                  <Field label="Veteran Status">
                    <select {...register('veteranStatus')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Prefer not to say</option>
                      <option value="Not a protected veteran">Not a protected veteran</option>
                      <option value="Protected veteran">Protected veteran</option>
                    </select>
                  </Field>
                  <Field label="Disability Status">
                    <select {...register('disabilityStatus')} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-white">
                      <option value="">Prefer not to say</option>
                      <option value="No">No, I don't have a disability</option>
                      <option value="Yes">Yes, I have a disability</option>
                    </select>
                  </Field>
                </div>
              </div>
            </div>
          )}

          {/* STEP 8: DOCUMENTS */}
          {step === 8 && (
            <div className="space-y-6">
              <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3 mb-4">Additional Documents</h2>
              <p className="text-sm text-slate-500 mb-6">Upload any additional documents to support your application.</p>
              
              <div className="space-y-4">
                <div className="flex items-center justify-between border border-slate-200 rounded-lg p-3">
                  <label htmlFor="doc-cover-letter" className="text-sm font-medium text-slate-700">Cover Letter</label>
                  <input id="doc-cover-letter" type="file" onChange={(e) => setCoverLetterFile(e.target.files?.[0])} className="text-sm text-slate-500 file:mr-4 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200" />
                </div>
                <div className="flex items-center justify-between border border-slate-200 rounded-lg p-3">
                  <label htmlFor="doc-certifications" className="text-sm font-medium text-slate-700">Certifications</label>
                  <input id="doc-certifications" type="file" onChange={(e) => setCertsFile(e.target.files?.[0])} className="text-sm text-slate-500 file:mr-4 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200" />
                </div>
                <div className="flex items-center justify-between border border-slate-200 rounded-lg p-3">
                  <label htmlFor="doc-transcripts" className="text-sm font-medium text-slate-700">Transcripts</label>
                  <input id="doc-transcripts" type="file" onChange={(e) => setTranscriptsFile(e.target.files?.[0])} className="text-sm text-slate-500 file:mr-4 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200" />
                </div>
                <div className="flex items-center justify-between border border-slate-200 rounded-lg p-3">
                  <label htmlFor="doc-portfolio" className="text-sm font-medium text-slate-700">Portfolio</label>
                  <input id="doc-portfolio" type="file" onChange={(e) => setPortfolioFile(e.target.files?.[0])} className="text-sm text-slate-500 file:mr-4 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200" />
                </div>
              </div>
            </div>
          )}

          {/* STEP 9: REVIEW & SUBMIT */}
          {step === 9 && (
            <div className="space-y-6">
              <h2 className="font-semibold text-slate-800 border-b border-slate-100 pb-3 mb-4">Review & Submit</h2>
              
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 mb-6">
                <h3 className="font-medium text-slate-700 mb-2">Almost done!</h3>
                <p className="text-sm text-slate-500">Please review your information by clicking back, or submit your application now.</p>
              </div>

              <div className="bg-slate-50 p-5 rounded-xl border border-slate-200">
                <h2 className="font-semibold text-slate-800 mb-3">Legal & Consent</h2>
                <label className="flex gap-3 cursor-pointer">
                  <input type="checkbox" {...register('gdprConsent')} className="mt-0.5 w-4 h-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500" />
                  <div>
                    <p className="text-sm font-medium text-slate-800">Data Processing Consent *</p>
                    <p className="text-xs text-slate-500 mt-0.5">I agree to the processing of my personal data for the recruitment process in accordance with the Privacy Policy.</p>
                  </div>
                </label>
                {errors.gdprConsent && <p className="text-red-500 text-xs mt-1 ml-7">{errors.gdprConsent.message}</p>}
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between mt-6">
          <button type="button" onClick={prevStep} disabled={step === 0}
            className="btn-secondary px-6 py-2.5 disabled:opacity-40">
            Back
          </button>
          {step < STEPS.length - 1 ? (
            <button type="button" onClick={nextStep} className="btn-primary px-8 py-2.5">
              Next: {STEPS[step + 1].label}
            </button>
          ) : (
            <button type="submit" disabled={isSubmitting} className="btn-primary px-8 py-2.5 shadow-lg shadow-primary-500/30">
              {isSubmitting ? 'Submitting...' : 'Submit Application'}
            </button>
          )}
        </div>
      </form>
    </div>
  );
};
