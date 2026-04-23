"use client";
import React, { useEffect, useRef } from "react";
import grapesjs from "grapesjs";
import "grapesjs/dist/css/grapes.min.css";
import gjsPresetNewsletter from "grapesjs-preset-newsletter";

export default function EmailEditor({ initialHtml, onHtmlChange }) {
  const editorRef = useRef(null);
  const editorInstance = useRef(null);

  useEffect(() => {
    // Initialize GrapesJS only once
    if (!editorInstance.current) {
      editorInstance.current = grapesjs.init({
        container: editorRef.current,
        fromElement: true,
        height: "700px",
        width: "100%",
        storageManager: false, // We rely on our Spring Boot database instead
        plugins: [gjsPresetNewsletter],
        pluginsOpts: {
          [gjsPresetNewsletter]: {
            modalTitleImport: "Import template",
          },
        },
      });

      // Listen for changes in the drag-and-drop editor and send the updated code back to Next.js
      editorInstance.current.on("update", () => {
        const html = editorInstance.current.getHtml();
        const css = editorInstance.current.getCss();
        // Combine CSS and HTML for saving to the database
        onHtmlChange(`<style>${css}</style>\n${html}`);
      });
    }

    // Cleanup when component unmounts
    return () => {
      if (editorInstance.current) {
        editorInstance.current.destroy();
        editorInstance.current = null;
      }
    };
  }, []);

  // When AI generates new code, inject it into the visual canvas
  useEffect(() => {
    if (editorInstance.current && initialHtml) {
      const currentHtml = `<style>${editorInstance.current.getCss()}</style>\n${editorInstance.current.getHtml()}`;
      if (initialHtml !== currentHtml) {
        editorInstance.current.setComponents(initialHtml);
      }
    }
  }, [initialHtml]);

  return <div ref={editorRef}></div>;
}