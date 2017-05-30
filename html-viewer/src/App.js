import React, { Component } from 'react';
import Suite from './components/Suite'
import SuitesList from './components/SuitesList'
import TestItem from './components/TestItem'
import {
  HashRouter as Router,
  Route,
  Switch
} from 'react-router-dom'

class App extends Component {
  render() {
    return (
      <Router>
        <Switch>
          <div className="page">
            <div className="page-content">
              <Route exact path="/" component={ SuitesList } />
              <Route exact path="/suites/:suiteId" component={ Suite } />
              <Route path="/suites/:suiteId/tests/:testId" component={ TestItem } />
            </div>
          </div>
        </Switch>
      </Router>
    );
  }
}

export default App;
