import React, { Component } from 'react';
import getJson from './../utils/getJson';
import convertTime from './../utils/convertTime'
import { Link } from 'react-router-dom';
import SearchBar from './SearchBar';

export default class Suite extends Component {
  state = {
    data: null
  };

  componentWillMount() {
    console.log('Suite');
    getJson(`/data/suites/${this.props.match.params.suiteId}.json`, (json) => {
      console.log(JSON.parse(json))
      this.setState({ data: JSON.parse(json) })
    });
  }

  render() {
    let { data } = this.state;
    if (!data) return null;

    return (
      <div className="content margin-top-20">
        <div className="title-common"><Link to="/">Suits list</Link>/ Suite {data.id}</div>
        <div className="row justify-between">
          <div className="card card-info">
            <div className="text-sub-title-light">Passed</div>
            <div className="card-info__content">{ data.passed_count }</div>
          </div>
          <div className="card card-info">
            <div className="text-sub-title-light">Failed</div>
            <div className="card-info__content">
              <div className="text-important">{ data.failed_count }</div>
            </div>
          </div>
          <div className="card card-info">
            <div className="text-sub-title-light">Ignored</div>
            <div className="card-info__content">
              <div className="text-grey">{ data.ignored_count }</div>
            </div>
          </div>
          <div className="card card-info">
            <div className="text-sub-title-light">Duration</div>
            <div className="card-info__content">{ convertTime(data.duration_millis) }</div>
          </div>
        </div>

        <SearchBar />

        <div className="card">
          <div className="title-common">Tests <span className="label">{ data.tests.length }</span></div>
          <div className="container-expanded list">
            { data.tests.map((test, i) => {
              return ( <Link key={ i } to={`${this.props.match.url}/tests/${test.id}`} className={`list__item ${test.status}`}>
                <div className="list__item__title-m text-sub-title">{ test.name }</div>
                <div className="list__item__title-l margin-right-10">{ test.class_name }</div>
                <div className="list__item__title-s">{ test.package_name }</div>
                <div className="additional-info">
                  <div className="label info margin-right-10">{ test.deviceId }</div>
                  <div className="label">{ convertTime(test.duration_millis) }</div>
                </div>
              </Link> )
            }) }
          </div>
        </div>
      </div>
    );
  }
}