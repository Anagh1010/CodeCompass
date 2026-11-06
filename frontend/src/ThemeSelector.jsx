import { useEffect, useState } from "react";
import { Palette } from "lucide-react";

const themes = [
  { id: "midnight", label: "Midnight" },
  { id: "graphite", label: "Graphite" },
  { id: "ocean", label: "Ocean" },
  { id: "forest", label: "Forest" },
  { id: "paper", label: "Paper" },
  { id: "sand", label: "Sand" },
  { id: "lavender", label: "Lavender" },
];

export default function ThemeSelector() {
  const [theme, setTheme] = useState(() => localStorage.getItem("codecompass-theme") || "midnight");

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("codecompass-theme", theme);
  }, [theme]);

  return (
    <label className="theme-picker">
      <Palette size={15} aria-hidden="true" />
      <span className="visually-hidden">Color theme</span>
      <select value={theme} onChange={(event) => setTheme(event.target.value)} aria-label="Color theme">
        {themes.map((option) => <option key={option.id} value={option.id}>{option.label}</option>)}
      </select>
    </label>
  );
}
