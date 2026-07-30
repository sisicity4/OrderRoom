import { useNavigate } from 'react-router-dom'

function App() {
  const navigate= useNavigate()
  return (
    <div className="flex flex-col gap-4 p-8">
      <h1>オーダールーム</h1>
      <button onClick={() => navigate('/create')} className="bg-blue-500 text-white px-4 py-2 rounded">作成</button>
      <button onClick={()=> navigate('/join')} className="bg-green-500 text-white px-4 py-2 rounded">参加</button>
    </div>
  )
}

export default App
