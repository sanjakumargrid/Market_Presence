import { Upload, FileText, Trash2, AlertCircle } from 'lucide-react';
import { useRef, useState } from 'react';

interface Props {
  value?: File;
  onChange: (file: File | undefined) => void;
}

const ALLOWED_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];
const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx'];

function isAllowed(file: File): boolean {
  const typeOk = ALLOWED_TYPES.includes(file.type);
  const ext = file.name.toLowerCase().slice(file.name.lastIndexOf('.'));
  const extOk = ALLOWED_EXTENSIONS.includes(ext);
  return typeOk || extOk;
}

export const ResumeUploader = ({ value, onChange }: Props) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [typeError, setTypeError] = useState('');

  const handleFile = (file: File) => {
    if (isAllowed(file)) {
      setTypeError('');
      onChange(file);
    } else {
      setTypeError(
        `"${file.name}" is not supported. Please upload a PDF or DOCX file.`
      );
      onChange(undefined);
      // Reset the native input so the same invalid file triggers onChange again if re-selected
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div>
      {!value ? (
        <>
          <div
            role="button"
            tabIndex={0}
            aria-label="Upload resume — drag and drop or click to browse"
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragging(false);
              const f = e.dataTransfer.files[0];
              if (f) handleFile(f);
            }}
            onClick={() => inputRef.current?.click()}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click(); }}
            className={`border-2 border-dashed rounded-xl p-10 text-center cursor-pointer transition-colors focus:outline-none focus:ring-2 focus:ring-primary-400 ${
              dragging
                ? 'border-primary-500 bg-primary-50'
                : typeError
                  ? 'border-red-400 bg-red-50'
                  : 'border-slate-300 hover:border-primary-400 hover:bg-slate-50'
            }`}
          >
            <Upload aria-hidden="true" className="w-10 h-10 text-slate-400 mx-auto mb-3" />
            <p className="text-sm font-medium text-slate-700 mb-1">Drag and drop your resume</p>
            <p className="text-xs text-slate-400">PDF or DOCX · up to 10 MB</p>
            <input
              ref={inputRef}
              type="file"
              accept=".pdf,.doc,.docx"
              aria-label="Browse for resume file"
              className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f); }}
            />
          </div>

          {typeError && (
            <div
              role="alert"
              className="mt-2 flex items-start gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2"
            >
              <AlertCircle aria-hidden="true" className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{typeError}</span>
            </div>
          )}
        </>
      ) : (
        <div className="border border-slate-200 rounded-xl p-4 flex items-center gap-3">
          <FileText aria-hidden="true" className="w-8 h-8 text-primary-600 shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-slate-800 truncate">{value.name}</p>
            <p className="text-xs text-slate-400">{(value.size / 1024 / 1024).toFixed(1)} MB</p>
            <div className="mt-1.5 h-1.5 bg-slate-100 rounded-full overflow-hidden" aria-hidden="true">
              <div className="h-full bg-primary-500 rounded-full w-full" />
            </div>
          </div>
          <button
            type="button"
            aria-label="Remove resume"
            onClick={() => { setTypeError(''); onChange(undefined); }}
            className="text-slate-400 hover:text-red-500 transition-colors focus:outline-none focus:ring-2 focus:ring-red-400 rounded"
          >
            <Trash2 aria-hidden="true" className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
};
