import * as React from 'react';
import { browserHistory } from 'react-router';
import { url } from '../../util';
import { login } from '../../util/auth';

interface ILoginPageState {
  username: string;
  password: string;
  error: string;
  loading: boolean;
}

export default class LoginPage extends React.Component<{}, ILoginPageState> {
  constructor(props) {
    super(props);
    this.state = {
      username: '',
      password: '',
      error: '',
      loading: false
    };
  }

  handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ username: e.target.value });
  }

  handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ password: e.target.value });
  }

  handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    this.setState({ loading: true, error: '' });

    try {
      const response = await fetch(url('api/auth/login'), {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: this.state.username,
          password: this.state.password
        })
      });

      if (response.ok) {
        const data = await response.json();
        login(this.state.username, this.state.password, data.roles);
        browserHistory.push('/');
        window.location.reload();
      } else {
        const errorData = await response.json();
        this.setState({ error: errorData.message || 'Login failed' });
      }
    } catch (err) {
      this.setState({ error: 'Network error. Please try again.' });
    } finally {
      this.setState({ loading: false });
    }
  }

  render() {
    const { username, password, error, loading } = this.state;

    return (
      <div className="container">
        <div className="row">
          <div className="col-md-6 col-md-offset-3">
            <h2>Login</h2>
            <br />
            {error && (
              <div className="alert alert-danger">
                <span className="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>
                &nbsp;{error}
              </div>
            )}
            <form onSubmit={this.handleSubmit}>
              <div className="form-group">
                <label htmlFor="username">Username</label>
                <input
                  type="text"
                  className="form-control"
                  id="username"
                  placeholder="Enter username"
                  value={username}
                  onChange={this.handleUsernameChange}
                  required
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label htmlFor="password">Password</label>
                <input
                  type="password"
                  className="form-control"
                  id="password"
                  placeholder="Enter password"
                  value={password}
                  onChange={this.handlePasswordChange}
                  required
                  disabled={loading}
                />
              </div>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                {loading ? 'Logging in...' : 'Login'}
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }
}

