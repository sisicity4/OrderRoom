import { useNavigate } from 'react-router-dom'

function App() {
  const navigate= useNavigate()
  return (
    <div className="flex flex-col gap-4 p-8 bg-[#f0e5cc] min-h-screen">
      <h3 className='tracking-[1.6em] text-neutral-500'>注文票作成アプリ</h3>
      <p className='tracking-[0.5em] text-neutral-500'>おーだーるーむ</p>
      <h1 className='text-4xl font-bold pb-5'>オーダールーム</h1>

      <hr className='border-t-5'/ >

      <div onClick={() => navigate('/create')} 
      className='flex items-center gap-6 cursor-pointer border-b border-dashed border-neutral-700 py-8'>
      <p className='text-2xl text-[#7A6B57]'>01</p>
      <div className='flex flex-col'>
        <h2 className='text-5xl'>作 成</h2>
        <p className='tracking-[0.4em]'>新しい注文票を作成</p>
        <p className='tracking-[0.4em]'>あたらしく でんぴょうを ひらく</p>
        </div>

      </div>
       <div onClick={() => navigate('/join')} className='flex items-center gap-6 cursor-pointer border-b border-dashed border-neutral-700 py-8'>
        <p className='text-2xl text-[#7A6B57]' >02</p>
        <div className='flex flex-col'>
          <h2 className='text-5xl'>参 加</h2>
        <p className='tracking-[0.4em]'>既存の注文票に相席する</p>
        <p className='tracking-[0.4em]'>あいせき ・ URL / QR</p>
        </div>
      </div>
      
    </div>
  )
}

export default App
