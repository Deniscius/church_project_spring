import React from 'react';

export default function AppSelect({ options = [], ...props }) {
  return (
    <select className="select" {...props}>
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}
