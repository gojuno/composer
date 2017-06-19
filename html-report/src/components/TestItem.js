import React, { Component } from 'react';
import cx from 'classnames';
import convertTime from './../utils/convertTime'
import paths from './../utils/paths'

export default class TestItem extends Component {
  componentWillMount() {
    document.title = `Test ${window.test.name}`;
  }

  render() {
    const data = window.test;
    let statusLabelClass = cx('label', 'margin-right-10', {
      alert: data.status === 'failed',
      success: data.status === 'passed'
    });
    let statusTextClass = cx('test-page__title', {
      'status-failed': data.status === 'failed',
      'status-ignored': data.status === 'ignored',
      'status-passed': data.status === 'passed'
    });
    let blockClass = cx('card row full justify-between test-page', {
      'failed': data.status === 'failed',
      'ignored': data.status === 'ignored',
      'passed': data.status === 'passed'
    });
    return (
      <div className="content margin-top-20">
        <div className="title-common vertical-aligned-content">
          <a href={ paths.fromTestToIndex }>Suits list</a> /
          <a href={ paths.fromTestToSuite(data.suite_id) }>Suite { data.suite_id }</a> /
          { data.deviceId }
        </div>
        <div className='margin-top-20'>
          <div className={ blockClass }>
            <div className="margin-right-20">
              <div className="margin-bottom-10 vertical-aligned-content">
                <div className={ statusLabelClass }>{ data.status }</div>
                <span className={ statusTextClass }>{ data.name }</span></div>
              <div className="title-l text-sub-title margin-bottom-5">{ data.class_name }</div>
              <div className="margin-bottom-5">{ data.package_name }</div>
            </div>
            <div className="card-info__content">{ convertTime(data.duration_millis) }</div>
          </div>

          { !!Object.keys(data.properties).length && <div className="card">
            <div className="row">
              { Object.keys(data.properties).map((keyName, i) => ( <div key={ i } className="text-block col-20">
                <div className="text-block__title text-sub-title-light">{`${keyName}:`}</div>
                <div className="text-block__content text-sub-title">data.properties[keyName]</div>
              </div> )) }
            </div>
          </div> }

          { !!data.file_paths.length && <div className="card">
            <div className="title-common">
              Files
            </div>
            { data.file_paths.map((file, i) => <div key={ i } className="margin-bottom-20">
              <div className="shortened">
                <a href={ file }>{ file }</a>
              </div>
            </div>) }
          </div> }

          { !!data.screenshots_paths.length && <div className="card">
            <ul className="images row">
              { data.screenshots_paths.map((image) => {
                return ( <li key={ image } className="images__item col-20">
                  <img src={ image } />
                </li> )
              }) }
            </ul>
          </div>}
        </div>
      </div>
    );
  }
}
