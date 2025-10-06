import React, { useState } from 'react';
import './ToggleSwitch.css'; // Assurez-vous de fournir le bon chemin vers votre fichier CSS

const ToggleSwitch = ({switchFunction, SwitchState}) => {
  const [isChecked, setChecked] = useState(SwitchState);

  const handleToggle = () => {
    setChecked(!isChecked);
  };

  return (
    <label className="switch">
      <input type="checkbox" checked={isChecked} onChange={handleToggle} onClick={switchFunction} />
      <span className={`slider round`}></span>
    </label>
  );
};

export default ToggleSwitch;
