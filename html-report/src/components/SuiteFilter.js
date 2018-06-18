import React, { Component } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import convertTime from './../utils/convertTime'
import SearchBar from './SearchBar';

const SEARCH_FIELDS = ['package_name', 'class_name', 'name', 'id', 'status'];
const SEARCH_REF = 'id';
export default class SuiteFilter extends Component {
  static propTypes = {
    setSearchResults: PropTypes.func
  };

  performFilterSearch = (query) => {
    this.searchBar.performFilterSearch(query)
  };

  render() {
    const data = window.suite;

    return (
      <div>
        <div className="row justify-between">
          <div className="card card-info filter-card" onClick={ () => this.performFilterSearch('status:passed') }>
            <div className="text-sub-title-light">Passed</div>
            <div className="card-info__content status-passed">{ data.passed_count }</div>
          </div>
          <div className="card card-info filter-card" onClick={ () => this.performFilterSearch('status:failed') }>
            <div className="text-sub-title-light">Failed</div>
            <div className="card-info__content status-failed">{ data.failed_count }</div>
          </div>
          <div className="card card-info filter-card" onClick={ () => this.performFilterSearch('status:ignored') }>
            <div className="text-sub-title-light">Ignored</div>
            <div className="card-info__content status-ignored">{ data.ignored_count }</div>
          </div>
          <div className="card card-info">
            <div className="text-sub-title-light">Duration</div>
            <div className="card-info__content">{ convertTime(data.duration_millis) }</div>
          </div>
        </div>
        <SearchBar
          setSearchResults={this.props.setSearchResults}
          searchFields={SEARCH_FIELDS}
          searchRef={SEARCH_REF}
          data={data.tests}
          setPerformFilterSearchCallback={ callback => (this.performFilterSearch = callback) }
          />
      </div>
    )
  }
}
