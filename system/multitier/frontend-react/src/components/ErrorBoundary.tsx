import { Component, type ErrorInfo, type ReactNode } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * Error Boundary component to catch JavaScript errors anywhere in the component tree
 * Displays a fallback UI instead of crashing the entire application
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null
    };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    // Update state so the next render will show the fallback UI
    return {
      hasError: true,
      error
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log error details for debugging
    console.error('Error Boundary caught an error:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null
    });
  };

  render() {
    if (this.state.hasError) {
      // Render custom fallback UI if provided
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Default fallback UI
      return (
        <div className="container mt-5">
          <div className="card shadow">
            <div className="card-header bg-danger text-white">
              <h4 className="mb-0">Something went wrong</h4>
            </div>
            <div className="card-body">
              <p className="mb-3">
                We're sorry, but something unexpected happened. The error has been logged.
              </p>
              {this.state.error && (
                <div className="alert alert-secondary">
                  <strong>Error Details:</strong>
                  <pre className="mb-0 mt-2" style={{ fontSize: '0.875rem' }}>
                    {this.state.error.message}
                  </pre>
                </div>
              )}
              <div className="d-flex gap-2">
                <button
                  className="btn btn-primary"
                  onClick={this.handleReset}
                >
                  Try Again
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={() => globalThis.location.href = '/'}
                >
                  Go to Home
                </button>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
