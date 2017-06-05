import React, { Component } from 'react';
import cx from 'classnames';
import convertTime from './../utils/convertTime'

export default class TestItem extends Component {
  render() {
    const data = window.test;
    let statusLabelClass = cx('label', 'margin-right-10', {
      alert: data.status === 'failed',
      success: data.status === 'passed'
    });
    let statusTextClass = cx({
      'status-failed': data.status === 'failed',
      'status-ignored': data.status === 'ignored',
      'status-passed': data.status === 'passed'
    });
    return (
      <div className="content margin-top-20">
        <div className="title-common"><a href="../../../index.html">Suits list</a> / <a href={ `../../${data.suiteId}.html` }>
          Suite { data.suiteId }</a> /
          <div className="label info">{ data.deviceId }</div>
        </div>
        <div className="margin-top-20">
          <div className="card row full justify-between">
            <div className="margin-right-20">
              <div className="text-sub-title margin-bottom-10">
                <div className={ statusLabelClass }>{ data.status }</div>
                <span className={ statusTextClass }>{ data.name }</span></div>
              <div className="title-l text-sub-title margin-bottom-5">{ data.class_name }</div>
              <div className="margin-bottom-5">{ data.package_name }</div>
            </div>
            <div className="labels-list">
              <div className="label">{ convertTime(data.duration_millis) }</div>
            </div>
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

          <div className="card">
            <ul className="images row">
              { data.screenshots_paths.map((image) => {
                return ( <li key={ image } className="images__item col-20">
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