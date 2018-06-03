import React, { Component } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import LogFilter from './LogFilter'

export default class LogContainer extends Component {
  static propTypes = {
    logcatPath: PropTypes.string
  };

  state = {
    logs: [],
    results: [],
    loading: true,
    hide: false
  }

  componentDidMount() {
      this.readSingleFile(this.props.logcatPath)
  }

  readSingleFile(path) {
    fetch(path)
      .then(response =>
        response.status == 200
        ? response.text()
        : this.setState({hide: true, loading: false})
      )
      .then(text => text.split('\n'))
      .then(lines => lines.map(line =>
        Object.assign({level: this.logLevel(line), line: line, key: this.hashCode(line)}))
      )
      .then(logs => {
        this.setState({logs: logs, results: logs, loading: false});
        window.document.getElementById("static_logs").remove();
      })
      .catch(error => this.setState({hide: true, loading: false}))
  };

  getSearchResults(results) {
    this.setState({ results: results });
  }

  hashCode(str) {
    return str.split('').reduce((prevHash, currVal) =>
      (((prevHash << 5) - prevHash) + currVal.charCodeAt(0))|0, 0);
  }

  logLevel(line) {
    let match = line.match(/^[^A-Z]*?([A-Z])/);
    if (match == null || match.length < 2) {
        return "default";
    }
    switch (match[1]) {
      case "V": return "verbose";
      case "D": return "debug";
      case "I": return "info";
      case "W": return "warning";
      case "E": return "error";
      case "A": return "assert";
      default: return "default";
    }
  }

  render() {
    return this.state.hide
    ? (<div></div>)
    : (
      <div className="margin-top-20">
        <LogFilter
          setSearchResults={ (results) => this.getSearchResults(results) }
          data={this.state.logs}
          />
        { !this.state.loading &&
        <div className="card log">
          { this.state.results.map((entry, i) => {
            return (
              <div key={ entry.key } className={"log__" + entry.level}>
                {entry.line}
              </div>
            )
          })
          }
        </div>}
      </div>
    );
  }
}
