import React, { Component } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import elasticlunr from 'elasticlunr';
import convertTime from './../utils/convertTime'

const EL_SEARCH = elasticlunr();
export default class SearchBar extends Component {
  static propTypes = {
    setSearchResults: PropTypes.func,
    setPerformFilterSearchCallback: PropTypes.func,
    searchFields: PropTypes.array,
    searchRef: PropTypes.string,
    data: PropTypes.array,
  };

  state = {
    data: this.props.data,
    error: false,
    searchLabel: null,
    searchParams: null,
    query: ''
  };

  componentWillMount() {
    elasticlunr.clearStopWords();
    this.props.searchFields.forEach(f => EL_SEARCH.addField(f))
    EL_SEARCH.setRef(this.props.searchRef);
  }

  componentDidMount() {
    this.props.setPerformFilterSearchCallback(this.performFilterSearch)
  }

  componentWillReceiveProps(props) {
      let data = props.data;
      if (data === this.state.data) {
        return;
      }
      if (data.length) {
        data.forEach(item => EL_SEARCH.addDoc(item))
      }
      this.setState({data: props.data});
  }

  componentWillUnmount() {
    this.props.setPerformFilterSearchCallback(undefined)
  }

  mapResults(results) {
    return results.map(item => {
      return EL_SEARCH.documentStore.docs[item.ref];
    })
  }

  clearResults = () => {
    this.props.setSearchResults(this.state.data);
    this.setState({ searchLabel: null, searchParams: null, error: false, query: '' });
  };

  setTagSearch = (field, callback) => {
    if (this.props.searchFields.indexOf(field) < 0) {
      this.setState({ error: true });
      return;
    }

    let params = {};
    params.fields = {};
    this.props.searchFields.forEach((f) => {
      if (f === field) {
        params.fields[f] = { boost: 1 }
      } else {
        params.fields[f] = { boost: 0 }
      }
    });

    this.setState({ searchLabel: field, searchParams: params, query: '' }, callback)
  };

  performSearch = (query) => {
    let searchParameters = { expand: true };
    if (this.state.searchParams) {
      Object.assign(searchParameters, this.state.searchParams)
    }
    let results = EL_SEARCH.search(query, searchParameters);
    this.props.setSearchResults(this.mapResults(results))
  };

  performFilterSearch = (query) => {
    const splitData = query.split(':');
    this.setTagSearch(splitData[0], () => {
      this.performSearch(splitData[1]);
      this.setState({ query: splitData[1] });
    });
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
          <div className="row search-params full">
            <div className="row full-width-content input-group full">
              <div className="form-item">
                <div className="vertical-aligned-content">
                  { this.state.searchLabel && <div className="label margin-right-20">{ this.state.searchLabel }:</div> }
                  <input
                    type="text" className={ errorInputClasses } placeholder="Search" value={ this.state.query }
                    onChange={ this.setSearchQuery }
                    disabled={ !!!this.state.data.length ? "disabled" : "" }
                    />
                  <button
                    type="reset" className="button secondary margin-left-20"
                    onClick={ this.clearResults }
                    disabled={ !!!this.state.data.length ? "disabled" : "" }
                    >
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
