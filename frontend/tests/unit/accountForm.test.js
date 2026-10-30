import {
  isValidEmail,
  isStrongPassword,
  isUsernameLengthValid,
  validateLoginCredentials,
  validateRegisterInput
} from '../../src/utils/accountForm'

describe('accountForm 登录/注册表单校验纯逻辑', () => {
  describe('isValidEmail', () => {
    it('合法邮箱通过', () => {
      expect(isValidEmail('alice@example.com')).toBe(true)
      expect(isValidEmail('a.b+cd@example.co.cn')).toBe(true)
    })
    it('非法邮箱被拒绝', () => {
      expect(isValidEmail('')).toBe(false)
      expect(isValidEmail(null)).toBe(false)
      expect(isValidEmail('no-at-sign')).toBe(false)
      expect(isValidEmail('a@b')).toBe(false)
      expect(isValidEmail('a b@c.com')).toBe(false)
      expect(isValidEmail('@example.com')).toBe(false)
    })
  })

  describe('isStrongPassword', () => {
    it('同时含字母和数字通过', () => {
      expect(isStrongPassword('abc12345')).toBe(true)
      expect(isStrongPassword('Abcd1234')).toBe(true)
    })
    it('纯字母/纯数字被拒绝', () => {
      expect(isStrongPassword('abcdefgh')).toBe(false)
      expect(isStrongPassword('12345678')).toBe(false)
      expect(isStrongPassword('')).toBe(false)
    })
  })

  describe('isUsernameLengthValid', () => {
    it('3-20 字符通过', () => {
      expect(isUsernameLengthValid('abc')).toBe(true)
      expect(isUsernameLengthValid('abcdefghijklmnopqrst')).toBe(true)
    })
    it('过短/过长被拒绝', () => {
      expect(isUsernameLengthValid('ab')).toBe(false)
      expect(isUsernameLengthValid('abcdefghijklmnopqrstu')).toBe(false)
      expect(isUsernameLengthValid('')).toBe(false)
    })
  })

  describe('validateLoginCredentials', () => {
    it('空用户名或空密码返回失败提示', () => {
      expect(validateLoginCredentials({ username: '', password: 'x' })).toMatchObject({ valid: false })
      expect(validateLoginCredentials({ username: 'u', password: '' })).toMatchObject({ valid: false })
      expect(validateLoginCredentials({}).valid).toBe(false)
    })
    it('两者均非空通过', () => {
      expect(validateLoginCredentials({ username: 'alice', password: 'pw' })).toMatchObject({ valid: true, message: '' })
    })
  })

  describe('validateRegisterInput', () => {
    const validForm = {
      username: 'alice01',
      email: 'alice@example.com',
      password: 'abc12345',
      confirmPassword: 'abc12345',
      agreed: true
    }
    const okAge = { valid: true, message: '', age: null }

    it('全部合法时通过', () => {
      expect(validateRegisterInput(validForm, okAge)).toMatchObject({ valid: true })
    })

    it('未勾选协议优先报错', () => {
      const r = validateRegisterInput({ ...validForm, agreed: false }, okAge)
      expect(r.valid).toBe(false)
      expect(r.message).toContain('协议')
    })

    it('必填字段缺失报错', () => {
      expect(validateRegisterInput({ ...validForm, email: '' }, okAge).message).toBe('请填写所有必填字段')
    })

    it('用户名长度边界', () => {
      expect(validateRegisterInput({ ...validForm, username: 'ab' }, okAge).message).toContain('用户名')
      expect(validateRegisterInput({ ...validForm, username: 'a'.repeat(21) }, okAge).message).toContain('用户名')
    })

    it('邮箱格式错误', () => {
      expect(validateRegisterInput({ ...validForm, email: 'bad-email' }, okAge).message).toContain('邮箱')
    })

    it('密码长度边界', () => {
      expect(validateRegisterInput({ ...validForm, password: 'a1', confirmPassword: 'a1' }, okAge).message).toContain('密码长度')
      expect(validateRegisterInput({ ...validForm, password: 'a1'.repeat(11), confirmPassword: 'a1'.repeat(11) }, okAge).message).toContain('密码长度')
    })

    it('密码强度不足', () => {
      expect(validateRegisterInput({ ...validForm, password: 'abcdefgh', confirmPassword: 'abcdefgh' }, okAge).message).toContain('字母和数字')
    })

    it('两次密码不一致', () => {
      expect(validateRegisterInput({ ...validForm, confirmPassword: 'abc12346' }, okAge).message).toContain('一致')
    })

    it('年龄校验失败时透传提示', () => {
      const badAge = { valid: false, message: '年龄输入无效', age: null }
      expect(validateRegisterInput(validForm, badAge)).toMatchObject({ valid: false, message: '年龄输入无效' })
    })
  })
})
