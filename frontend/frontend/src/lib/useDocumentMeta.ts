import { useEffect } from 'react';

interface DocumentMeta {
  title: string;
  description?: string;
  jsonLd?: Record<string, unknown>;
}

/**
 * Manages per-page document metadata for SEO (REQ-JP-09).
 *
 * - Sets `document.title` and restores the previous title on unmount.
 * - Injects/removes a `<meta name="description">` tag.
 * - Injects/removes a `<script type="application/ld+json">` tag.
 *
 * Works without react-helmet. React 19 natively hoists <title> and <meta>
 * from JSX — this hook handles the JSON-LD script which needs direct DOM
 * access, plus it keeps the meta tag in sync with the component lifecycle.
 */
export function useDocumentMeta({ title, description, jsonLd }: DocumentMeta) {
  useEffect(() => {
    const previous = document.title;
    document.title = title;
    return () => { document.title = previous; };
  }, [title]);

  useEffect(() => {
    if (!description) return;
    let meta = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    const created = !meta;
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'description';
      document.head.appendChild(meta);
    }
    const prev = meta.content;
    meta.content = description;
    return () => {
      if (meta) {
        if (created) {
          document.head.removeChild(meta);
        } else {
          meta.content = prev;
        }
      }
    };
  }, [description]);

  useEffect(() => {
    if (!jsonLd) return;
    const script = document.createElement('script');
    script.type = 'application/ld+json';
    script.textContent = JSON.stringify(jsonLd);
    document.head.appendChild(script);
    return () => {
      if (document.head.contains(script)) document.head.removeChild(script);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(jsonLd)]);
}
