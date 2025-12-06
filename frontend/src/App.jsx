import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'

function App() {
  const [count, setCount] = useState(0)

  function verify(){
    console.log("Verification sent");
  }

  return (
    <>
      <button onClick={verify()}>Zweryfikuj stronę</button>
    </>
  )
}

export default App
