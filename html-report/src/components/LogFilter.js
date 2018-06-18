import React, { Component } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import SearchBar from './SearchBar';
import LogFilterButton from './LogFilterButton';

const SEARCH_FIELDS = ['level', 'line'];
const SEARCH_REF = 'line';
export default class LogFilter extends Component {
  static propTypes = {
    setSearchResults: PropTypes.func,
    data: PropTypes.array
  };

  render() {
    return (
      <div>
        <div className="card">
          <div className="title-common">LOGS</div>
          <LogFilterButton onClick={ () => this.performFilterSearch("level:verbose") } text="Verbose" disabled={ !!!this.props.data.length } />
          <LogFilterButton onClick={ () => this.performFilterSearch("level:debug") } text="Debug" disabled={ !!!this.props.data.length } />
          <LogFilterButton onClick={ () => this.performFilterSearch("level:info") } text="Info" disabled={ !!!this.props.data.length } />
          <LogFilterButton onClick={ () => this.performFilterSearch("level:warning") } text="Warning" disabled={ !!!this.props.data.length } />
          <LogFilterButton onClick={ () => this.performFilterSearch("level:error") } text="Error" disabled={ !!!this.props.data.length } />
          <LogFilterButton onClick={ () => this.performFilterSearch("level:assert") } text="Assert" disabled={ !!!this.props.data.length } />
        </div>
        <SearchBar
          setSearchResults={this.props.setSearchResults}
          searchFields={SEARCH_FIELDS}
          searchRef={SEARCH_REF}
          data={this.props.data}
          setPerformFilterSearchCallback={ callback => (this.performFilterSearch = callback) }
          />
      </div>
    );
  }
}
