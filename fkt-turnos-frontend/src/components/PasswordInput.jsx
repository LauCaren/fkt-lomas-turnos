import { useState } from 'react';

function IconOjo({ oculto }) {
  if (oculto) {
    return (
      <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a21.8 21.8 0 0 1 5.06-6.06M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 8 11 8a21.8 21.8 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
        <line x1="1" y1="1" x2="23" y2="23" />
      </svg>
    );
  }
  return (
    <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

// 128 acá tiene que coincidir con Patrones.PASSWORD_LONGITUD_MAXIMA del backend — evita que
// alguien escriba una contraseña larguísima para después chocar contra el límite recién al
// enviar el formulario (y de paso, defensa en profundidad contra el truncado silencioso de
// BCrypt a 72 bytes, ver el comentario en Patrones.java).
const LONGITUD_MAXIMA_PASSWORD = 128;

export default function PasswordInput({ id, name, value, onChange, required, minLength, disabled, autoFocus, placeholder, pattern, title }) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="password-field">
      <input
        id={id}
        name={name}
        type={visible ? 'text' : 'password'}
        required={required}
        minLength={minLength}
        maxLength={LONGITUD_MAXIMA_PASSWORD}
        disabled={disabled}
        autoFocus={autoFocus}
        placeholder={placeholder}
        pattern={pattern}
        title={title}
        value={value}
        onChange={onChange}
      />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
      >
        <IconOjo oculto={visible} />
      </button>
    </div>
  );
}
