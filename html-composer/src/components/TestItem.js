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
        <div className="title-common"><a href="../../../index.html">Suits list</a> / <a href={ `../../${0}.html` }>
          Suite { 0 }</a> /
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
                let url = image.replace('/opt/project/ci/internal/../../artifacts/composer-output', './../../..');
                return ( <li key={image} className="images__item col-20">
                  <img src={ url } />
                </li> )
              }) }
            </ul>
          </div>

          <div className="card log">
            <div className="log__default">
              05-29 15:36:04.976 4065 4477 ? LogTracker: [Rx-1-io-30] Analytics start timing event: Appearance /
              StartupTime
            </div>
            <div className="log__info">
              05-29 15:36:04.976 4065 4477 I LogTracker: [Rx-1-io-30] Analytics start timing event: Appearance /
              StartupTime
            </div>
            <div className="log__verbose">
              05-29 15:36:04.976 4065 4477 V LogTracker: [Rx-1-io-30] Analytics start timing event: Appearance /
              StartupTime
            </div>
            <div className="log__debug">
              05-29 15:36:04.977 4065 4389 D RiderApplication$initFabricAsync: [Rx-1-io-28] Google Play Services
              version: 10.2.98 (470-146496160)
            </div>
            <div className="log__error">
              05-29 15:36:04.983 1287 1287 E EGL_emulation: tid 1287: eglCreateSyncKHR(1669): error 0x3004
              (EGL_BAD_ATTRIBUTE)
            </div>
            <div className="log__warning">
              05-29 15:36:05.100 4065 4175 W NioEventLoop: Selector.select() returned prematurely 512 times in a row;
              rebuilding Selector io.netty.channel.nio.SelectedSelectionKeySetSelector@bac7eb3.
            </div>
            <div className="log__assert">
              05-29 15:36:06.100 4065 4175 A NioEventLoop: Selector.select() returned prematurely 512 times in a row;
              rebuilding Selector io.netty.channel.nio.SelectedSelectionKeySetSelector@bac7eb3.
            </div>
          </div>
        </div>
      </div>
    );
  }
}