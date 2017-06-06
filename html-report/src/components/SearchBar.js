import React, { Component } from 'react';

export default class SearchBar extends Component {
  render() {
    return (
      <div className="card">
        <div className="form-container">
          <form>
            <div className="row search-params full">
              <div className="row full-width-content input-group full">
                <div className="form-item col-25">
                  <input type="text" placeholder="Search" />
                </div>
              </div>
              <button type="submit" className="button margin-left-20">Search</button>
              <button type="reset" className="button secondary margin-left-20">Reset</button>
            </div>
          </form>
        </div>
      </div>
    )
  }
}
