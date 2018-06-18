import React, { Component } from 'react';
import cx from 'classnames';
import LogFilter from './LogFilter'

export default class LogContainer extends Component {

  state = {
    logs: [],
    results: [],
    loading: true,
    hide: false
  }

  componentDidMount() {
    document.addEventListener('DOMContentLoaded', (event) => {
      this.loadStaticLogs()
    })
  }

  loadStaticLogs() {
    let logDivs = document.querySelectorAll('#static_logs > div > div')
    if (!logDivs.length) {
      this.setState({hide: true, loading: false})
      return
    }
    let logs = []
    for (var div of logDivs) {
      logs.push(Object.assign({level: div.className, line: div.innerText, key: logs.length}))
    }
    this.setState({logs: logs, results: logs, loading: false})
    window.document.getElementById("static_logs").remove()
  }

  getSearchResults(results) {
    this.setState({ results: results });
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
              <div key={ entry.key } className={ entry.level }>
                { entry.line }
              </div>
            )
          })
          }
        </div>}
      </div>
    );
  }
}
