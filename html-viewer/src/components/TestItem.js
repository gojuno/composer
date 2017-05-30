import React, { Component } from 'react';
import getJson from './../utils/getJson'
import convertTime from './../utils/convertTime'
import { Link } from 'react-router-dom';

export default class TestItem extends Component {
  state = {
    data: null
  };

  componentWillMount() {
    let { match } = this.props;
    console.log('Test', match.params);
    getJson(`/data/suites/${match.params.suiteId}/${match.params.testId}.json`, (json) => {
      console.log(JSON.parse(json))
      this.setState({ data: JSON.parse(json) })
    });
  }

  render() {
    let { data } = this.state;
    let { match } = this.props;
    if (!data) return null;

    return (
      <div className="content margin-top-20">
        <div className="title-common"><Link to="/">Suits list</Link>/ <Link to={`/suites/${match.params.suiteId}`}> Suite { match.params.suiteId }</Link>/ Test { data.id }</div>
        <div className="content margin-top-20">
          <div className="row justify-between">
            <div className="card card-info">
              <div className="text-sub-title-light">Status</div>
              <div className="card-info__content">{ data.status }</div>
            </div>
            <div className="card card-info">
              <div className="text-sub-title-light">Class</div>
              <div className="card-info__content">
                <div className="text-important">{ data.class_name }</div>
              </div>
            </div>
            <div className="card card-info">
              <div className="text-sub-title-light">Duration</div>
              <div className="card-info__content">{ convertTime(data.duration_millis) }</div>
            </div>
          </div>

          <div className="card">
            <ul className="images row">
              { data.screenshots_paths.map((image) => {
                return ( <li key={image} className="images__item">
                  <img src={ image } />
                </li> )
              }) }
            </ul>
          </div>
        </div>
      </div>
    );
  }
}