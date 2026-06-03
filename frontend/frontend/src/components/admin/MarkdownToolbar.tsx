import type { RefObject } from 'react';

interface Props {
  textareaRef: RefObject<HTMLTextAreaElement | null>;
  onChange: (value: string) => void;
}

function insertWrapped(
  el: HTMLTextAreaElement,
  before: string,
  after: string,
  onChange: (v: string) => void
) {
  const start = el.selectionStart;
  const end = el.selectionEnd;
  const selected = el.value.slice(start, end) || 'text';
  const newValue =
    el.value.slice(0, start) + before + selected + after + el.value.slice(end);
  onChange(newValue);
  requestAnimationFrame(() => {
    el.focus();
    el.setSelectionRange(start + before.length, start + before.length + selected.length);
  });
}

function insertLinePrefix(
  el: HTMLTextAreaElement,
  prefix: string,
  onChange: (v: string) => void
) {
  const start = el.selectionStart;
  const lineStart = el.value.lastIndexOf('\n', start - 1) + 1;
  const newValue = el.value.slice(0, lineStart) + prefix + el.value.slice(lineStart);
  onChange(newValue);
  requestAnimationFrame(() => {
    el.focus();
    el.setSelectionRange(start + prefix.length, start + prefix.length);
  });
}

export const MarkdownToolbar = ({ textareaRef, onChange }: Props) => {
  const tools = [
    {
      label: 'B',
      title: 'Bold',
      action: () => textareaRef.current && insertWrapped(textareaRef.current, '**', '**', onChange),
    },
    {
      label: 'I',
      title: 'Italic',
      action: () => textareaRef.current && insertWrapped(textareaRef.current, '_', '_', onChange),
    },
    {
      label: '•',
      title: 'Bullet point',
      action: () => textareaRef.current && insertLinePrefix(textareaRef.current, '- ', onChange),
    },
    {
      label: 'H2',
      title: 'Section heading',
      action: () => textareaRef.current && insertLinePrefix(textareaRef.current, '## ', onChange),
    },
  ];

  return (
    <div className="flex items-center gap-1 mb-1">
      {tools.map((t) => (
        <button
          key={t.label}
          type="button"
          title={t.title}
          onClick={t.action}
          className="px-2 py-0.5 text-xs font-mono bg-slate-100 hover:bg-slate-200 border border-slate-200 rounded text-slate-600 transition-colors focus:outline-none focus:ring-1 focus:ring-primary-400"
        >
          {t.label}
        </button>
      ))}
      <span className="ml-2 text-[10px] text-slate-400">Markdown supported</span>
    </div>
  );
};
