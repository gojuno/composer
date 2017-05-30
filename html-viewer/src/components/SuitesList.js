import React, { Component } from 'react';
import getJson from './../utils/getJson';
import convertTime from './../utils/convertTime';
import { Link } from 'react-router-dom';

export default class SuitesList extends Component {
  state = {
    data: null
  };

  componentWillMount() {
    console.log('Suites list');
    getJson(`/data/index.json`, (json) => {
      this.setState({ data: JSON.parse(json) })
    });
  }

  render() {
    let { data } = this.state;

    if (!data) return null;
    return (
      <div className="content margin-top-20">
        <div className="title-common">Suit list</div>

        { data.suites.map((suite) => {
            return (
              <div key={ suite.id } className="suite-item card">

                <Link to={`/suites/${suite.id}`} className="title-common suite-item__title">
                  Suite { suite.id  }
                </Link>
                <div className="row full margin-bottom-20 w700">
                  <div className="card-info">
                    <div className="text-sub-title-light">Passed</div>
                    <div className="card-info__content text-success">{ suite.passed_count }</div>
                  </div>
                  <div className="card-info">
                    <div className="text-sub-title-light">Failed</div>
                    <div className="card-info__content">
                      <div className="text-important">{ suite.failed_count }</div>
                    </div>
                  </div>
                  <div className="card-info">
                    <div className="text-sub-title-light">Ignored</div>
                    <div className="card-info__content">
                      <div className="text-grey">{ suite.ignored_count }</div>
                    </div>
                  </div>
                  <div className="card-info">
                    <div className="text-sub-title-light">Duration</div>
                    <div className="card-info__content">{ convertTime(suite.duration_millis) }</div>
                  </div>
                  <div className="card-info">
                    <div className="text-sub-title-light">Devices</div>
                    <div className="card-info__content">{ suite.devices.length }</div>
                  </div>
                </div>
                <ul className="container-expanded list">
                  { suite.devices.map((device, i) => {
                      return (<li key={ i } className="list__item no-hover">
                          <div className="text-title margin-bottom-10 label">{ device.id }</div>
                          <div className="margin-bottom-10"><span className="hoverable">{ device.logcat_path }</span>
                          </div>
                          <div className="margin-bottom-10"><span className="hoverable">{ device.instrumentation_output_path }</span>
                          </div>
                        </li>
                      )
                    }
                  )}
                </ul>
              </div>
            )
          }
        )}
      </div>
    );
  }
}