import React, { Component } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import elasticlunr from 'elasticlunr';

const SEARCH_FIELDS = ['package_name', 'class_name', 'name', 'id', 'status'];
const SEARCH_REF = 'id';
const EL_SEARCH = elasticlunr();
const STATUSES = ['failed', 'passed', 'ignored'];
export default class SearchBar extends Component {
  static propTypes = {
    data: PropTypes.array,
    setSearchResults: PropTypes.func
  };

  state = {
    error: false,
    searchLabel: null,
    searchParams: null,
    query: ''
  };

  componentWillMount() {
    let { data } = this.props;
    elasticlunr.clearStopWords();
    SEARCH_FIELDS.forEach(f => EL_SEARCH.addField(f))
    EL_SEARCH.setRef(SEARCH_REF);
    if (data.length) {
      data.forEach(item => EL_SEARCH.addDoc(item))
    }
  }

  mapResults(results) {
    return results.map(item => {
      return EL_SEARCH.documentStore.docs[item.ref];
    })
  }

  clearResults = () => {
    this.props.setSearchResults(this.props.data);
    this.setState({ searchLabel: null, searchParams: null, error: false, query: '' });
  };

  filterByStatus = (status) => {
    this.setTagSearch('status');
    this.performSearch(status);
    this.setState({ query: status, error: false });
  };

  setTagSearch = (field) => {
    if (SEARCH_FIELDS.indexOf(field) < 0) {
      this.setState({ error: true });
      return;
    }

    let params = {};
    params.fields = {};
    SEARCH_FIELDS.forEach((f) => {
      if (f === field) {
        params.fields[f] = { boost: 1 }
      } else {
        params.fields[f] = { boost: 0 }
      }
    });

    this.setState({ searchLabel: field, searchParams: params, query: '' })
  };

  performSearch = (query) => {
    let searchParameters = { expand: true };
    if (this.state.searchParams) {
      Object.assign(searchParameters, this.state.searchParams)
    }
    let results = EL_SEARCH.search(query, searchParameters);
    this.props.setSearchResults(this.mapResults(results))
  };

  setSearchQuery = (event) => {
    let val = event.target.value;
    this.setState({ query: val, error: false });

    if (!val) {
      if (this.state.searchLabel) return;
      this.clearResults();
      return;
    }

    if (val.indexOf(':') < 0) {
      this.performSearch(val)
    } else {
      this.setTagSearch(val.split(':')[0])
    }
  };

  render() {
    let errorTextClasses = cx('form-item__error-text col-100', { visible: this.state.error });
    let errorInputClasses = cx({ 'is-invalid-input': this.state.error });

    return (
      <div className="card">
        <div className="form-container">
          <div className="filter-group row full margin-bottom-20">
            { STATUSES.map(s =>
              <div key={ s }
                   onClick={ () => this.filterByStatus(s) }
                   className={ cx('filter-group__item', { active: s == this.state.query }) }>
                { s }
              </div>)
            }
          </div>
          <div className="row search-params full">
            <div className="row full-width-content input-group full">
              <div className="form-item">
                <div className="vertical-aligned-content">
                  { this.state.searchLabel && <div className="label margin-right-20">{ this.state.searchLabel }:</div> }
                  <input type="text" className={ errorInputClasses } placeholder="Search" value={ this.state.query }
                         onChange={ this.setSearchQuery } />
                  <button type="reset" className="button secondary margin-left-20" onClick={ this.clearResults }>
                    Reset
                  </button>
                </div>
                <div className={ errorTextClasses }>No such key exists!</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
}
