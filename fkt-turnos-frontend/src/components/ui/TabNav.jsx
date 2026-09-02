export default function TabNav({ tabs, active, onChange }) {
  return (
    <div className="tabnav" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          aria-selected={active === t.id}
          className={`tabnav-btn${active === t.id ? ' active' : ''}`}
          onClick={() => onChange(t.id)}
        >
          {t.label}
        </button>
      ))}
    </div>
  );
}

export function TabPanel({ tabKey, children }) {
  return <div key={tabKey} className="page-fade">{children}</div>;
}
