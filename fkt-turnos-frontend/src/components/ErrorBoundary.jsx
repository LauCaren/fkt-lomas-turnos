import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Error no controlado en la UI:', error, info.componentStack);
  }

  handleReintentar = () => {
    this.setState({ error: null });
  };

  render() {
    if (!this.state.error) return this.props.children;

    return (
      <div className="wrap" style={{ paddingTop: 80, paddingBottom: 80, textAlign: 'center' }}>
        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 28, marginBottom: 12 }}>
          Algo salió mal
        </h1>
        <p style={{ color: 'var(--muted)', marginBottom: 24 }}>
          La página encontró un error inesperado. Podés intentar de nuevo o volver a cargarla.
        </p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-outline" onClick={this.handleReintentar}>
            Reintentar
          </button>
          <button type="button" className="btn btn-primary" onClick={() => window.location.assign('/')}>
            Volver al inicio
          </button>
        </div>
      </div>
    );
  }
}
