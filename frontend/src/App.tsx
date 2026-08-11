import { useNavigate } from 'react-router-dom'

function App() {
  const navigate= useNavigate()
  return (
    <div className="flex flex-col gap-4 p-8 bg-[#f0e5cc] min-h-screen">
      <h3 className='tracking-[0.5em] text-[#7A6B57]'>注文票作成アプリ</h3>
      <p className='tracking-[0.5em] text-[#7A6B57]'>おーだーるーむ</p>
      <h1 className='text-[clamp(1.75rem,7vw,2.5rem)] font-bold pb-5'>オーダールーム</h1>

      <hr className='border-t-5'/ >
      <div className='text-[#7A6B57] flex'>
        <p className='shrink-0'>品番</p>
        <p className='tracking-[1.5em] ml-6 shrink-0'>品目</p>
        <p className='ml-auto mr-5 shrink-0'>選択</p>
      </div>
      <hr />

      <div onClick={() => navigate('/create')}
          className='flex items-center gap-4 cursor-pointer border-b border-dashed border-neutral-700 py-8'>
          <p className='text-2xl text-[#7A6B57] shrink-0'>01</p>

        <div className='flex flex-col min-w-0'>
          <h2 className='text-[clamp(2rem,10vw,3rem)]'>作 成</h2>
          <p className='tracking-[0.2em]'>新しい注文票を作成</p>
        </div>

        <p className='[writing-mode:vertical-rl] border-2 rounded-full w-16 h-16 shrink-0 grid place-items-center -rotate-12 text-red-700 ml-auto font-bold'>開店</p>
      </div>

        <div onClick={() => navigate('/join')} 
          className='flex items-center gap-4 cursor-pointer border-b border-dashed border-neutral-700 py-8'>
          <p className='text-2xl text-[#7A6B57] shrink-0' >02</p>
          <div className='flex flex-col min-w-0'>
            <h2 className='text-[clamp(2rem,10vw,3rem)]'>参 加</h2>
            <p className='tracking-[0.2em]'>注文票に相席する</p>
         </div>

          <p className='[writing-mode:vertical-rl] border-2 text-red-700 rounded-full w-16 h-16 shrink-0 grid place-items-center -rotate-12 ml-auto font-bold'>相席</p>

        </div>

        <hr className='border-t-5' />

        <div className='flex'>
          <div>
            <p className='font-bold tracking-[0.2em] text-[#4A3F35]'>お品書き</p>
            <div className='tracking-[0.2em] text-[#7A6B57]'>
              <p>共同編集ができます。</p>
              <p>〆てから席をお立ちください。</p>
            </div>
          </div>
          <span className='text-[#7A6B57] ml-auto shrink-0 border w-20 h-20 grid place-items-center rotate-90 border-dashed'>割印</span>
        </div>
       
        
    </div>
  )
}

export default App
