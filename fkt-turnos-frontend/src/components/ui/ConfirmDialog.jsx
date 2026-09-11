import { useCallback, useRef, useState } from 'react';
import Modal from './Modal';
import { ConfirmContext } from './useConfirm';

/**
 * Reemplaza los `window.confirm(...)` que había repartidos por el código — sin estilo,
 * sin poder mostrar el turno/paciente concreto de la acción, y bloqueantes de forma fea
 * en la app de escritorio (Electron). Se monta una sola vez, cerca de la raíz (ver
 * App.jsx); cualquier componente adentro llama a useConfirm() (ver ./useConfirm.js).
 */
export function ConfirmProvider({ children }) {
  const [pedido, setPedido] = useState(null);

  const confirmar = useCallback((opciones) => new Promise((resolve) => {
    setPedido({ ...opciones, resolve });
  }), []);

  function resolver(resultado) {
    pedido?.resolve(resultado);
    setPedido(null);
  }

  return (
    <ConfirmContext.Provider value={confirmar}>
      {children}
      {pedido && (
        <ConfirmDialogView
          {...pedido}
          onConfirmar={() => resolver(true)}
          onCancelar={() => resolver(false)}
        />
      )}
    </ConfirmContext.Provider>
  );
}

function ConfirmDialogView({ titulo, detalle, confirmarTexto = 'Confirmar', cancelarTexto = 'Cancelar', peligroso, onConfirmar, onCancelar }) {
  // El foco por defecto va a "Cancelar", no a la acción que confirma — para una acción
  // destructiva (cancelar turno, desactivar cuenta, aprobar una baja permanente), un Enter
  // reflejo no debería ejecutarla.
  const cancelarRef = useRef(null);

  return (
    <Modal onCerrar={onCancelar} labelledBy="confirmDialogTitulo" initialFocusRef={cancelarRef}>
      <h2 id="confirmDialogTitulo" className="modal-title">{titulo}</h2>
      {detalle && <div className="modal-detail">{detalle}</div>}
      <div className="modal-actions">
        <button type="button" className="btn btn-outline" ref={cancelarRef} onClick={onCancelar}>{cancelarTexto}</button>
        <button type="button" className={`btn ${peligroso ? 'btn-dark' : 'btn-primary'}`} onClick={onConfirmar}>{confirmarTexto}</button>
      </div>
    </Modal>
  );
}
