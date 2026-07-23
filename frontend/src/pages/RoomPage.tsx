import { useState } from 'react'
function RoomPage(){
  const [text,setText]=useState('');
  const [list,setList]=useState<string[]>([]);
  
  return(
    <div className='bg-[#f0e5cc] min-h-screen flex flex-col'>
      <p className='tracking-[0.5em] text-[#7A6B57]'>おしながき</p>
      <h1 className='text-4xl font-bold'>タイトル</h1>
      <div className='flex'>
       <p className='border w-fit px-2 mb-3 '>ID.</p> 
       <p className='border px-2 w-fit mb-3'>予算</p>
      </div>
      
      
      <hr className='border-t-5' />
     
      <p className='border-b w-fit'>ここに作成時の予算表示</p>
      <p className='border w-fit px-2'>注文</p>
      <ul>
        {list.map((item, index) => (
          <li className='border-y border-dashed p-3'
           key={index}>{item}</li>
          ))}
      </ul>

      <input 
      className='border-b outline-none'
      value={text}
      placeholder='注文を追加'
      onChange={(e)=> setText(e.target.value)} />
      <button onClick={()=>{
        setList([...list,text]);
        setText('');
      }}>注文の追加</button>
      
      
    </div>

  );
  
}
export default RoomPage;