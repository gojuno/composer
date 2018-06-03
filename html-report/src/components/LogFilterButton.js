import React from 'react';

const LogFilterButton = ({text, disabled, onClick}) => (
  <button
    className="button secondary margin-right-10"
    onClick={ onClick }
    disabled={ disabled }
    >
    {text}
  </button>
);

export default LogFilterButton;
