"use client";
import React, { useEffect, useRef, forwardRef, useImperativeHandle } from "react";
import grapesjs from "grapesjs";
import "grapesjs/dist/css/grapes.min.css";
import gjsPresetNewsletter from "grapesjs-preset-newsletter";
// NOTE: gjsBlocksBasic removed — the newsletter preset includes email-optimized blocks

// Use forwardRef so templates/page.js can call loadTemplate() directly via a ref
const EmailEditor = forwardRef(function EmailEditor({ initialHtml, onHtmlChange }, ref) {
  const editorContainerRef = useRef(null);
  const editorInstance = useRef(null);

  // Expose the raw GrapesJS instance to the parent via useImperativeHandle
  useImperativeHandle(ref, () => ({
    loadTemplate: (html) => {
      if (editorInstance.current) {
        editorInstance.current.setComponents(html || "");
      }
    },
    getEditor: () => editorInstance.current,
  }));

  useEffect(() => {
    // Guard against React Strict Mode double-fire
    if (editorInstance.current) return;

    editorInstance.current = grapesjs.init({
      container: editorContainerRef.current,
      fromElement: false,
      height: "100%",
      width: "100%",
      storageManager: false,

      // Newsletter preset: email-optimized blocks + inline CSS for email clients
      plugins: [gjsPresetNewsletter],
      pluginsOpts: {
        [gjsPresetNewsletter]: {
          modalTitleImport: "Importer le code HTML",
          inlineCss: true,
          cellStyle: { padding: "0", margin: "0", "vertical-align": "top" },
        },
      },

      // Modernize the canvas background so the email frame is visually distinct
      canvas: {
        styles: ["body { background-color: #f8fafc; font-family: sans-serif; }"],
      },
    });

    const e = editorInstance.current;

    // --- UX ENHANCEMENTS ---

    e.on("load", () => {
      // 1. Remove useless developer buttons from the top toolbar
      const panels = e.Panels;
      panels.removeButton("options", "export-template");        // Remove 'View Code'
      panels.removeButton("options", "fullscreen");              // Remove Fullscreen
      panels.removeButton("options", "gjs-open-import-webpage"); // Remove Import page

      // 2. Open Block Manager by default so user sees the drag toolbox immediately
      const blockManagerBtn = e.Panels.getButton("views", "open-blocks");
      if (blockManagerBtn) blockManagerBtn.set("active", 1);

      // 3. Rename "Href" → "Lien URL" and translate target options to French
      //    Makes the Trait Manager marketer-friendly instead of developer-facing
      e.DomComponents.addType("link", {
        model: {
          defaults: {
            traits: [
              { type: "text",   name: "href",   label: "Lien URL" },
              {
                type: "select",
                name: "target",
                label: "Ouvrir",
                options: [
                  { value: "",       name: "Même page"    },
                  { value: "_blank", name: "Nouvel onglet" },
                ],
              },
            ],
          },
        },
      });
    });

    // 4. Auto-switch right panel based on selected component type
    //    Image/Link → Trait Manager (edit URL/src)  |  Other → Style Manager
    e.on("component:selected", (model) => {
      const type = model.get("type");
      if (type === "image" || type === "link") {
        const traitManagerBtn = e.Panels.getButton("views", "open-tm");
        if (traitManagerBtn) traitManagerBtn.set("active", 1);
      } else {
        const styleManagerBtn = e.Panels.getButton("views", "open-sm");
        if (styleManagerBtn) styleManagerBtn.set("active", 1);
      }
    });


    // --- Push HTML+CSS changes back to Next.js state on every canvas update ---
    e.on("update", () => {
      const html = e.getHtml();
      const css = e.getCss();
      onHtmlChange(`<style>${css}</style>\n${html}`);
    });

    // Cleanup on unmount
    return () => {
      if (editorInstance.current) {
        editorInstance.current.destroy();
        editorInstance.current = null;
      }
    };
  }, []);

  // When AI generates new HTML, inject it directly into the live canvas
  useEffect(() => {
    if (editorInstance.current && initialHtml !== undefined && initialHtml !== null) {
      editorInstance.current.setComponents(initialHtml);
    }
  }, [initialHtml]);

  return (
    <>
      <style>{`
        /* ═══════════════════════════════════════════════════════════
           SmartMail Pro — Aggressive GrapesJS Theme
           Uses !important to penetrate GrapesJS's internal specificity.
           All selectors scoped to .gjs-* so they never leak into the app.
        ═══════════════════════════════════════════════════════════ */

        /* Editor container font */
        .gjs-editor-cont { font-family: inherit !important; }

        /* Core color system — overrides .gjs-one-bg / .gjs-two-color etc. */
        .gjs-one-bg   { background-color: #0f172a !important; }  /* Slate 900 */
        .gjs-two-color { color: #cbd5e1 !important; }             /* Slate 300 */
        .gjs-three-bg  { background-color: #1e293b !important; color: white !important; } /* Slate 800 */
        .gjs-four-color, .gjs-four-color-h:hover { color: #2563eb !important; } /* Blue 600 */

        /* Toolbar buttons */
        .gjs-pn-btn {
          color: #94a3b8 !important;
          border-radius: 6px !important;
          margin: 2px !important;
          transition: background-color 0.15s, color 0.15s !important;
        }
        .gjs-pn-btn:hover { background-color: #334155 !important; color: #fff !important; }
        .gjs-pn-btn.gjs-pn-active {
          background-color: #2563eb !important;
          color: white !important;
          box-shadow: none !important;
        }

        /* Trait / Settings panel rows */
        .gjs-trt-trait {
          padding: 10px 15px !important;
          display: block !important;
          border-bottom: 1px solid #1e293b !important;
        }
        .gjs-label-wrp {
          margin-bottom: 5px !important;
          font-size: 11px !important;
          font-weight: 600 !important;
          text-transform: uppercase !important;
          letter-spacing: 0.06em !important;
          color: #94a3b8 !important;
        }

        /* All input / select fields inside GrapesJS */
        .gjs-field {
          background-color: #1e293b !important;
          border: 1px solid #334155 !important;
          border-radius: 6px !important;
          padding: 4px !important;
        }
        .gjs-field input, .gjs-field select {
          color: white !important;
          font-size: 13px !important;
          background: transparent !important;
        }

        /* Canvas surround */
        .gjs-cv-canvas {
          background-color: #e2e8f0 !important;
          padding: 20px !important;
        }

        /* Email frame shadow */
        .gjs-frame {
          box-shadow: 0 20px 25px -5px rgba(0,0,0,0.15), 0 10px 10px -5px rgba(0,0,0,0.06) !important;
          border-radius: 8px !important;
        }

        /* Block cards in the blocks panel */
        .gjs-block {
          border-radius: 8px !important;
          border: 1px solid #334155 !important;
          transition: border-color 0.2s, color 0.2s !important;
          color: #cbd5e1 !important;
        }
        .gjs-block:hover {
          border-color: #2563eb !important;
          color: #2563eb !important;
        }

        /* Style Manager sector titles */
        .gjs-sm-sector .gjs-sm-title {
          background-color: #1e293b !important;
          border-bottom: 1px solid #334155 !important;
          font-weight: 600 !important;
          letter-spacing: 0.05em !important;
        }

        /* Hide GrapesJS logo & remove panel separator lines */
        .gjs-logo-cont { display: none !important; }
        .gjs-pn-panel  { border-bottom: none !important; }
      `}</style>

      {/* GrapesJS mounts here — isolated empty div, no other React children */}
      <div
        ref={editorContainerRef}
        className="w-full h-full rounded-2xl overflow-hidden"
      />
    </>
  );
});

export default EmailEditor;